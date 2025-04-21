package server;

import application.AMOApplication;
import application.Application;
import application.KVStore;
import client.Reply;
import client.Request;
import networking.Heartbeat;
import networking.Message;
import networking.MessageBody;
import results.Result;
import commands.*;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
    private final ArrayList<String> serverAddresses;

    private final ArrayList<String> clientAddresses;

    // ip addr to server id
    private final Map<String, Integer> serverIpToId;
    // server id to ip addr
    private final Map<Integer, String> serverIdToIp;

    // ip addr to client id
    private final Map<String, Integer> clientIpToId;
    // client id to ip addr
    private final Map<Integer, String> clientIdToIp;

    private final String myIp;
    private final int myListenPort;
    private final int serverId;
    private ServerSocket serverSocket;

    private int hbInterval = 1750; // 5/3 seconds

    // last heartbeat tarcking     
    private final Map<Integer, Long> lastHeartbeatReceived;

    private String failureType = "";


    // PAXOS INFORMATION HERE
    //--------------//--------------//--------------//--------------//--------------//
    // PMMC 7 states
    // ρ.state: The replica’s copy of the application state, which we will treat as opaque. All
    //replicas start with the same initial application state.
    private Application app;


    /***** A Proposer"s State *****/

    // ρ.slot in: The index of the next slot in which the replica has not yet proposed any
    // command, initially 1.
    private int slotIn;


    // ρ.slot out: The index of the next slot for which it needs to learn a decision before it
    // can update its copy of the application state, equivalent to the state’s version number
    // (i.e., number of updates) and initially 1.
    private int slotOut;


    // ρ.requests: An initially empty set of requests that the replica has received and are
    // not yet proposed or decided.
    private Set<Command> requests;


    // ρ.proposals: An initially empty set of proposals that are currently outstanding.
    private Set<Propose> proposals;


    // ρ.decisions: Another set of proposals that are known to have been decided (also
    //initially empty).
    private Set<Propose> decisions;


    private HashSet<Integer> prepared = new HashSet<Integer>();
    // nodes that have prepared for us
    private Multimap<LogEntry, Integer> acceptorsOfLogEntry =
            ArrayListMultimap.create(); // nodes that have accepted for us
    /***** A Proposer's State Ends *****/


    /**** An Acceptor's State ****/
    // α.accepted, a set of pvalues, initially empty.
    private Set<PValue> accepted;

    /**** An Acceptor's State Ends ****/


    /**** Local Permanent State ****/
    // the LocalLog
    private Log log;


    // current time in clock ticks
    private int time;


    // keep track of last leader_ping_time
    private int last_leader_ping_time;


    private Integer self;


    private int last_non_cleared = 0;


    // round number
    private int current_round;


    int lastUndecidedSlots[];


    int garbageCollected;
    //

    int startWithPaxos = 0;

    int currentLeader = -1; // the current leader in the round
    int previousLeader = -1; // the previous leader in the round

    //--------------//--------------//--------------//--------------//--------------//

    String messageFileName;

    public Server(Application app, ArrayList<String> serverAddresses, ArrayList<String> clientAddresses, String myCurrentIpAddr, int myListenPort, Map<String, Integer> serverIpToId, Map<String, Integer> clientIpToId) {
        this.app = app;
        this.serverAddresses = serverAddresses;
        this.clientAddresses = clientAddresses;
        this.myIp = myCurrentIpAddr;
        this.myListenPort = myListenPort;
        this.serverId = serverIpToId.get(myIp + ":" + myListenPort);
        this.self = serverId;
        this.serverIpToId = serverIpToId;
        this.serverIdToIp = new HashMap<>();
        messageFileName = "server_" + serverId + "_messages.txt"; // for logging messages sent and received
        // clear the messageFileName
        try {
            FileWriter fw = new FileWriter(messageFileName, false);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(""); // clear the file
            bw.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to write to file: " + e.getMessage());
        }


        for (Map.Entry<String, Integer> entry : serverIpToId.entrySet()) {
            serverIdToIp.put(entry.getValue(), entry.getKey());
        }
        
        lastHeartbeatReceived = new HashMap<>();
        for (String address : serverAddresses) {
            String[] parts = address.split(":");
            int id = serverIpToId.get(address);
            lastHeartbeatReceived.put(id, System.currentTimeMillis());
        }

        // get the client ip to id
        this.clientIpToId = clientIpToId;
        this.clientIdToIp = new HashMap<>();
        for (Map.Entry<String, Integer> entry : clientIpToId.entrySet()) {
            clientIdToIp.put(entry.getValue(), entry.getKey());
        }

        /* PAXOS INIT */
        this.requests = new LinkedHashSet<>();
        this.proposals = new LinkedHashSet<>();
        this.decisions = new LinkedHashSet<>();
        this.log = new Log(new TreeMap<Integer, LogEntry>());

        this.slotIn = 1;
        this.slotOut = 1;

        /*** Garbaage Collection ***/
        this.lastUndecidedSlots = new int[serverAddresses.size()];
        for (int i = 0; i < serverAddresses.size(); i++) {
            lastUndecidedSlots[i] = 1;
        }
        this.garbageCollected = 1;
        /*** Garbaage Collection Ends ***/

        this.time = 0;

        broadcast(new Heartbeat(lastUndecidedSlots, slotOut, 0, current_round, current_round == 0 && self == 0));

        /* An Acceptor's Initial State */
        this.current_round = 0;
        this.accepted = new HashSet<PValue>();

        // clear the file
        String filename = "server_" + serverId + "_app.txt";
        try {
            FileWriter fw = new FileWriter(filename, false);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("");
            bw.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to write to file: " + e.getMessage());
        }
        
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(myListenPort);
            System.out.println("[SERVER " + serverId + "] Listening on port " + myListenPort);

            // new Thread(this::startHeartbeat).start();
            // start a heartbeat timer that checks for timeouts
            new Thread(this::heartbeatCheck).start();
            new Thread(this::startHeartbeat).start();
            new Thread(this::listenForMessages).start();
            new Thread(this::readFailureType).start();
        } catch (IOException e) {
            System.err.println("[SERVER " + serverId + "] Error starting server: " + e.getMessage());
        }
    }

    private void readFailureType() {
        while (true) {
            try {
                Thread.sleep(300); // Check every 0.3 seconds
                try (BufferedReader br = new BufferedReader(new FileReader("failures.txt"))) {
                    String line;
                    StringBuilder sb = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    failureType = sb.toString().trim();
                } catch (IOException e) {
                    e.printStackTrace();
                    // Handle the exception as needed
                }

            } catch (InterruptedException e) {
                System.err.println("[SERVER " + serverId + "] Error reading failure type: " + e.getMessage());
            }
        }
    }

    private void startHeartbeat() {
        while (true) {
            int firstKey = log.getLog().size() == 0 ? slotOut - 1 : log.getLog().firstKey();
            
            try {
                Thread.sleep(hbInterval); // Heartbeat interval
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            broadcast(new Heartbeat(lastUndecidedSlots, slotOut, firstKey, current_round, currentLeader == self));

        }
    }

    private void heartbeatCheck() {
        while(true) {
            
            // print the leader and round number
            // System.out.println("[SERVER " + serverId + "] Checking for heartbeat timeouts...");
            writeUpdateToFile();
            this.time++;
            // System.out.println("[SERVER " + serverId + "] Time: " + time);
            lastUndecidedSlots[self] = slotOut;
            if (failureType.contains("Leader") && failureType.contains(String.valueOf(serverId))) {
                // this means that the server itself is the leader that failed
                // do not process any messages from the leader
            } else if (leader_of(self, current_round)) {
                sendProposalsAgain();
                // System.out.println(self + " leader. sending " + Arrays.toString(lastUndecidedSlots));
                // garbageCollect(arrMin(lastUndecidedSlots));
                int firstKey = log.getLog().size() == 0 ? slotOut - 1 : log.getLog().firstKey();
                broadcast(new Heartbeat(lastUndecidedSlots, slotOut, firstKey, current_round, true));
                executeCommand();

                if (!majority(prepared)) {
                    prepared.add(self);
                    // send the prepared message to the servers not in the prepared list

                    for (String address : serverAddresses) {
                        int id = serverIpToId.get(address);
                        if (!prepared.contains(id) || id != self) {
                            sendMessageToServer(id, new Prepare(current_round));
                        }
                    }
                }
            } else {
                if (time > last_leader_ping_time + 1) {
                    // System.out.println(self + " says, Leader " + currentLeader + " timed out, changing round from " + current_round + " to " + (current_round + 1) + ".");
                    change_round(current_round + 1);
                    // make myself the leader
                    currentLeader = self;
                    if (leader_of(self, current_round)) {
                        // // System.out.println("Leader " + self + " is broadcasting prepare.");
                        prepared.add(self);
                        // System.out.println("[SERVER " + serverId + "] Leader " + self + " is broadcasting prepare for round " + current_round);
                        broadcast(new Prepare(current_round));
                    }
                }
            }
            try {
                Thread.sleep(hbInterval); // Heartbeat interval
            } catch (InterruptedException e) {
                System.err.println("[SERVER " + serverId + "] Error in heartbeat check: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    private void sendMessage(Message message) {
        String messageType = message.getMessageBody().getClass().getSimpleName();
        // the failure is in the type Link leader - adjacent failed
        // this means that the server should not process any messages from the adjacent server
        String senderIpPort = message.getReceiverIp() + ":" + message.getReceiverPort();
        // check if the senderIpPort is in the failure type
        // get teh server id from the senderIpPort
        int senderServerId = serverIpToId.get(senderIpPort);
        if (senderServerId == self) {
            throw new IllegalArgumentException("Sender server ID cannot be the same as the current server ID. Sender ID: " + senderServerId + ", Current ID: " + self + ", Message: " + message + " Sender IP: " + message.getSenderIp() + ", Sender Port: " + message.getSenderPort() + ", Receiver IP: " + message.getReceiverIp() + ", Receiver Port: " + message.getReceiverPort());
        }

        if (failureType.contains("Link")) {
            if (failureType.contains(String.valueOf(serverId)) && failureType.contains(String.valueOf(senderServerId))) {
                // this means that the sender is the one that failed
                // do not process this message
                // System.out.println("[SERVER " + serverId + "] Not sending message to SERVER " + senderServerId + " because it is in the failure type: " + failureType);
                return;
            }
        } else if (failureType.contains("Leader") && failureType.contains(String.valueOf(serverId))) {
            // this means that the server itself is the leader that failed
            // do not process any messages from the leader
            return;
        }

        // send the message to the appropriate server
        try (Socket socket = new Socket(message.getReceiverIp(), message.getReceiverPort());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                out.writeObject(message);
                out.flush();

                String receiverIpPort = message.getReceiverIp() + ":" + message.getReceiverPort();

                //  Messages received by node 1 from the previous leader (PL), new leader (NL), or another follower (F)
                String messageSenderType = "";
                if (senderServerId == currentLeader) {
                    messageSenderType = "CL"  ; // Current Leader
                } else if (senderServerId == previousLeader) {
                    messageSenderType = "PL"; // Previous Leader
                } else {
                    messageSenderType = "F"; // Follower
                }
                String messageString = "[SERVER " + serverId + "] Sent " + messageType + " to SERVER " + serverIpToId.get(receiverIpPort);
                System.out.println(messageString);

                writeMessageToFile(messageString + " | " + messageSenderType);
            } catch (IOException e) {
                String receiverIpPort = message.getReceiverIp() + ":" + message.getReceiverPort();
                System.err.println("[SERVER " + serverId + "] Error sending message to SERVER " + serverIpToId.get(receiverIpPort) + ": " + e.getMessage());
            }
    }

    private void listenForMessages() {
        while (true) {
            receiveMessage();
        }
    }

    private void receiveMessage() {
        try (Socket clientSocket = serverSocket.accept();
             ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

            Message message = (Message) in.readObject();

            processReceivedMessage(message);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[SERVER " + serverId + "] Error processing message: " + e);
        }
    }

    private void processReceivedMessage(Message message) {
        // message from client
        String senderIpPort = message.getSenderIp() + ":" + message.getSenderPort();
        int receieverServerId = serverIpToId.get(senderIpPort);

        // read the fail re type from the message if it exists
        // check if failureType contains Link:
        if (failureType.contains("Link")) {
            // check if the senderIpPort is in the failure type
            // get teh server id from the senderIpPort
            int senderServerId = serverIpToId.get(senderIpPort);
            if (failureType.contains(String.valueOf(serverId)) && failureType.contains(String.valueOf(senderServerId))) {
                // this means that the sender is the one that failed
                // do not process this message
                return;
            }
        } else if (failureType.contains("Leader") && failureType.contains(String.valueOf(serverId))) {
                // this means that the server itself is the leader that failed
                // do not process any messages from the leader
                return;
        } 

        if (clientAddresses.contains(senderIpPort)){
            processReceivedMessageFromClient(message);
            return;
        }

        String messageString = "";
        if (message.getMessageBody() instanceof Heartbeat) {
            handleHeartbeat((Heartbeat) message.getMessageBody(), senderIpPort);
            messageString = "[SERVER " + serverId + "] Received Heartbeat from SERVER " + serverIpToId.get(senderIpPort);
            System.out.println(messageString);
        } else if (message.getMessageBody() instanceof Prepare) {
            Prepare prepare = (Prepare) message.getMessageBody();
            int senderServer = serverIpToId.get(senderIpPort);
            handlePrepare(prepare, senderServer);
            messageString = "[SERVER " + serverId + "] Received Prepare from SERVER " + senderServer;
            System.out.println(messageString);
        } else if (message.getMessageBody() instanceof Prepared) {
            Prepared prepared = (Prepared) message.getMessageBody();
            int senderServer = serverIpToId.get(senderIpPort);
            handlePrepared(prepared, senderServer);
            messageString = "[SERVER " + serverId + "] Received Prepared from SERVER " + senderServer;
            System.out.println(messageString);
        } else if (message.getMessageBody() instanceof FirstUndecidedSlot) {
            FirstUndecidedSlot firstUndecidedSlot = (FirstUndecidedSlot) message.getMessageBody();
            int senderServer = serverIpToId.get(senderIpPort);
            messageString = "[SERVER " + serverId + "] Received FirstUndecidedSlot from SERVER " + senderServer;
            handleFirstUndecidedSlot(firstUndecidedSlot, senderServer);
            System.out.println(messageString);
        } else if (message.getMessageBody() instanceof BatchCatchupDecide) {
            BatchCatchupDecide batchCatchupDecide = (BatchCatchupDecide) message.getMessageBody();
            int senderServer = serverIpToId.get(senderIpPort);
            handleBatchCatchupDecide(batchCatchupDecide, senderServer);
            messageString = "[SERVER " + serverId + "] Received BatchCatchupDecide from SERVER " + senderServer;
            System.out.println(messageString);
        } else if (message.getMessageBody() instanceof CatchupDecide) {
            CatchupDecide catchupDecide = (CatchupDecide) message.getMessageBody();
            int senderServer = serverIpToId.get(senderIpPort);
            handleCatchupDecide(catchupDecide, senderServer);
            messageString = "[SERVER " + serverId + "] Received CatchupDecide from SERVER " + senderServer;
            System.out.println(messageString);
        } else if (message.getMessageBody() instanceof Propose) {
            Propose propose = (Propose) message.getMessageBody();
            int senderServer = serverIpToId.get(senderIpPort);
            handlePropose(propose, senderServer);
            messageString = "[SERVER " + serverId + "] Received Propose from SERVER " + senderServer;
            System.out.println(messageString);
        } else if (message.getMessageBody() instanceof Accept) {
            Accept accept = (Accept) message.getMessageBody();
            int senderServer = serverIpToId.get(senderIpPort);
            handleAccept(accept, senderServer);
            messageString = "[SERVER " + serverId + "] Received Accept from SERVER " + senderServer;
            System.out.println(messageString);
        } else if (message.getMessageBody() instanceof Decide) {
            Decide decide = (Decide) message.getMessageBody();
            int senderServer = serverIpToId.get(senderIpPort);
            handleDecide(decide, senderServer);
            messageString = "[SERVER " + serverId + "] Received Decide from SERVER " + senderServer;
            System.out.println(messageString);
        }
        
        else {
            messageString = "[SERVER " + serverId + "] Received UNKNOWN MESSAGE TYPE from SERVER " + senderIpPort;
            System.out.println(messageString);
        }

        //  Messages received by node 1 from the previous leader (PL), new leader (NL), or another follower (F)
        String messageSenderType = "";
        if (receieverServerId == currentLeader) {
            messageSenderType = "CL"  ; // Current Leader
        } else if (receieverServerId == previousLeader) {
            messageSenderType = "PL"; // Previous Leader
        } else {
            messageSenderType = "F"; // Follower
        }

        writeMessageToFile(messageString + " | " + messageSenderType);

    }

    private void handleHeartbeat(Heartbeat m, String senderIpPort) {
        int senderServer = serverIpToId.get(senderIpPort);

        if(change_round(m.getRound())) {
            if (m.isLeader()) {
                if (senderServer != previousLeader) {
                    previousLeader = senderServer;
                }
                currentLeader = senderServer;
            }
        }
        int sender = serverIpToId.get(senderIpPort);
        if (currentLeader == senderServer) {
            currentLeader = sender;
            last_leader_ping_time = time;
        }

        int firstKey = log.getLog().size() == 0 ? slotOut - 1 : log.getLog().firstKey();

        if (m.getFirstKey() > firstKey && m.getFirstKey() < slotOut) {
            // garbageCollect(m.getFirstKey());
        }
        int[] receivedSlots = m.getLatestUndecidedSlots();
        for (int i = 0; i < receivedSlots.length; i++) {
            lastUndecidedSlots[i] = Math.max(lastUndecidedSlots[i], receivedSlots[i]);
        }

        sendMessageToServer(senderServer, new FirstUndecidedSlot(slotOut));

        // print last undecided slots
        // System.out.println("Last Undecided Slots: " + Arrays.toString(lastUndecidedSlots));
    }

    private void processReceivedMessageFromClient(Message message) {
        AMOCommand amoCommand = (AMOCommand) message.getMessageBody();
        System.out.println("[SERVER " + serverId + "] Received Command from Server C | F");
        if (!leader_of(self, current_round)) {
            return;
        }

        int a = proposals.size();
        if (!existsInProposals(amoCommand)) {
            requests.add(amoCommand);
        }
        // print out requests
        // System.out.println("[SERVER " + serverId + "] Requests: " + requests.toString());

        if (majority(prepared)) {
            // send proposal while up to n instances are not decided
            while (requests.size() > 0) {
                send_proposal();
            }
        } else {
            if (!majority(prepared) && leader_of(self, current_round)) {
                // // System.out.println("Leader: Not sending cuz no majority prepared");
            }
        }

    }

    private void sendMessageToServer(int serverId, MessageBody messageBody) {
        String ipPort = serverIdToIp.get(serverId);
        String[] parts = ipPort.split(":");
        String ip = parts[0];
        int port = Integer.parseInt(parts[1]);
        if (serverId == self) {
            processReceivedMessage(new Message(myIp, myListenPort, myIp, myListenPort, messageBody));
            return;
        }
        Message message = new Message(myIp, myListenPort, ip, port, messageBody);
        sendMessage(message);
    }

    private void sendMessageToClient(int clientId, MessageBody messageBody) {
        String ipPort = clientIdToIp.get(clientId);
        String[] parts = ipPort.split(":");
        String ip = parts[0];
        int port = Integer.parseInt(parts[1]);
        Message message = new Message(myIp, myListenPort, ip, port, messageBody);
        sendMessage(message);
    }


    public static void main(String[] args) {
        try {
            // Create server for this computer setup like this reading from "serverIpToId.txt"
            // Servers 3
            // 128.83.144.122:66345_0
            // 128.83.144.123:66345_1
            // 128.83.144.2:66345_3

            String myCurrentIpAddr = Inet4Address.getLocalHost().getHostAddress();
            int myPort = -1;

            // data structures to hold server and client addresses
            ArrayList<String> serverAddresses = new ArrayList<>();
            ArrayList<String> clientAddresses = new ArrayList<>();
            Map<String, Integer> serverIpToId = new HashMap<>();

            // read the first word which is servers and the number of servers
            // then loop through the number of servers until you find the ip address that matches this computer
            // then read the port number and id of this server
            // all in the format ip:port_id_number

            // setup the file reading and line parsing
            BufferedReader br = new BufferedReader(new FileReader("serverIpToId.txt"));
            String line = br.readLine();
            System.out.println("[INFO] Reading server configuration from file: " + line);
            String[] parts = line.split(" ");
            int numberOfServers = Integer.parseInt(parts[1]);

            for (int i = 0; i < numberOfServers; i++) {
                line = br.readLine();
                String[] ipPortId = line.split("_");
                String[] ipPort = ipPortId[0].split(":");
                String ip = ipPort[0];
                int port = Integer.parseInt(ipPort[1]);
                String ipPortStr = ip + ":" + port;

                serverAddresses.add(ipPortStr);
                if (ip.equals(myCurrentIpAddr)) {
                    myPort = port;
                }
                int id = Integer.parseInt(ipPortId[1]);
                serverIpToId.put(ipPortStr, id);
            }

            assert myPort != -1 : "Server port not found for IP: " + myCurrentIpAddr;

            // read the client addresses
            line = br.readLine();
            System.out.println("[INFO] Reading client configuration from file: " + line);
            String[] clientParts = line.split(" ");
            int numberOfClients = Integer.parseInt(clientParts[1]);
            Map<String, Integer> clientIpToId = new HashMap<>();
            int clientId = -1;
            for (int i = 0; i < numberOfClients; i++) {
                line = br.readLine();
                String[] ipPortId = line.split("_");
                clientId = Integer.parseInt(ipPortId[1]);
                String[] ipPort = ipPortId[0].split(":");
                String ip = ipPort[0];
                int port = Integer.parseInt(ipPort[1]);
                String ipPortStr = ip + ":" + port;
                clientAddresses.add(ipPortStr);
                if (ip.equals(myCurrentIpAddr)) {
                    myPort = port;
                }
                clientIpToId.put(ipPortStr, clientId);
            }

            // print out both the server and client addresses
            System.out.println("[INFO] Server Addresses: " + serverIpToId);
            System.out.println("[INFO] Client Addresses: " + clientIpToId);
            // close the file reader
            br.close();

            // create an application instance
            KVStore kv = new KVStore();
            AMOApplication app = new AMOApplication(kv);

            Server server = new Server(app, serverAddresses, clientAddresses, myCurrentIpAddr, myPort, serverIpToId, clientIpToId);


            new Thread(server::startServer).start();


            System.out.println("[INFO] Server started on dynamic ports " + myPort);
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to allocate ports: " + e.getMessage());
        }
    }


    // --------------//--------------//--------------//--------------//--------------//
    // PAXOS IMPLEMENTATION -- PMMC 7 -- //
    private boolean change_round(int r) {
        if (current_round < r) {
            System.out.println("[SERVER " + serverId + "] Changing round from " + current_round + " to " + r);


            current_round = r;
            // reset the per-round state when moving to new round
            requests = new LinkedHashSet<>();
            proposals = new LinkedHashSet<Propose>();
            decisions = new LinkedHashSet<Propose>();
            // give new leader a lease
            prepared = new LinkedHashSet<Integer>();
            acceptorsOfLogEntry = ArrayListMultimap.create();
            // reset the last leader ping time
            last_leader_ping_time = time;
            currentLeader = -1;
        }
        return current_round == r;
    }

    private void executeCommand() {
        // local log object
        // loop through the log from slotout through the rest of thekeys of the log
        // get the key set of the log
        // loop through the key set of the log from slotout to the end
        int indexOfSlotOut = indexOfSlotOut(slotOut);
        ArrayList<Integer> keys = new ArrayList<Integer>(log.getLog().keySet());
        Collections.sort(keys);

        if (indexOfSlotOut == -1) {
            // // System.out.println(self + ": No slots to execute.");
            return;
        }

        // INSTABLE
        for (int i = indexOfSlotOut; i < keys.size(); i++) {
            int logSlot = keys.get(i);
            LogEntry logEntry = log.getLog().get(logSlot);
            if (logEntry.getCommand().getCommand() instanceof NoOp) {
                break;
            }
            if (logEntry.getStatus() != PaxosLogSlotStatus.CHOSEN) {
                break;
            }
            slotOut = Math.max(slotOut, slotOut + 1);
            // System.out.println("Executing: " + logEntry.getCommand().getCommand().toString());

            AMOCommand amoUnwrappedCommand = logEntry.getCommand();
            Command unwrappedCommand = amoUnwrappedCommand.getCommand();


            Reply reply =
                    new Reply(app.execute(amoUnwrappedCommand));

            //            replyCache = reply;
            //            clientCache = amoUnwrappedCommand.client();

            // // System.out.println(self + " Executing: " + unwrappedCommand + " Log: " + log.toString() + " SlotIn/SlotOut (next not proposed/learn a decision before updating state): " + slotIn + "/" + slotOut);
            if (leader_of(self, current_round)) {
                // respond to the client
                // send the reply to the client
                // sendMessageToClient(myIp, reply);
                sendMessageToClient((int) amoUnwrappedCommand.getClientId(), reply);
                

                // // System.out.println(self + " App State: " + app + ". Sending reply to client." + reply);
                // // System.out.println(self + " Sending reply to client " + amoUnwrappedCommand.client() + " . " + reply);

            } 
        }

    }

    private boolean leader_of(Integer address, int round) {
        // check if the address is the leader of the round
        if (round < 0) {
            return false;
        }
        // boolean isLeader = (address == (round % serverAddresses.size()));
        boolean isLeader = currentLeader == address;
        // System.out.println("[SERVER " + serverId + "] Is leader of round " + round + ": " + isLeader + " serverAddresss.size(): " + serverAddresses.size() + " address: " + address);
        // if is leader, clear the file and write my id to leader.txt
        if (isLeader && address == self) {
            try {
                FileWriter fw = new FileWriter("leader.txt", false);
                // System.out.println("[SERVER " + serverId + "] Writing to leader.txt: " + serverId + " . round " + round);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(String.valueOf(serverId));
                bw.close();
            } catch (IOException e) {
                System.err.println("[ERROR] Failed to write to leader.txt: " + e.getMessage());
            }
        }
        return isLeader;
    }

    private void handlePropose(Propose m, int sender) {
        // Your code here...
        // change round asserts that its the current round
        // ADDED CODE SEE IF MAKES A DIFF
        if (status(m.getVote().getSlot()) == PaxosLogSlotStatus.CHOSEN) {
            return;
        }
        if (change_round(m.getVote().getRound())) {
            // local log
            // put in accepted map
            LogEntry entry = m.getVote();
            acceptorsOfLogEntry.put(entry, self);
            putInMultiMap(acceptorsOfLogEntry, entry, self);
            putInMultiMap(acceptorsOfLogEntry, entry, sender);

            // generate log entry, in ACCEPTED
            LogEntry myEntry =
                    new LogEntry(entry.getSlot(), entry.getRound(), entry.getCommand(),
                            PaxosLogSlotStatus.ACCEPTED);

            addToMap(log.getLog(), myEntry, entry.getSlot());
            slotIn = calculateSlotIn();

            // deal with slotIn, make it the last slot in the map
            slotIn = calculateSlotIn();

            // handle propose list
            proposals.add(m);

            // send ACCEPT message
            if (sender != self) {
                // send(new Accept(entry), sender);
                sendMessageToServer(sender, new Accept(entry));
            }
            // // System.out.println("Server " + self + " Accepted " + m + " Log: " + log.toString() + " My Acceptance: " + acceptorsOfLogEntry );
        }
    }

    private void handleAccept(Accept m, int sender) {
        // send decide messages
        if (!decided(m.getVote().getSlot()) & change_round(m.getVote().getRound())) {
            putInMultiMap(acceptorsOfLogEntry, m.getVote(), sender);
            Collection<Integer> accept = acceptorsOfLogEntry.get(m.getVote());
            // print acceptors of log entry
            if (majority(accept)) { 
                // Deal with LOG state
                LogEntry myEntry =
                        new LogEntry(m.getVote().getSlot(), m.getVote().getRound(),
                                m.getVote().getCommand(), PaxosLogSlotStatus.ACCEPTED);
                addToMap(log.getLog(), myEntry, myEntry.getSlot());
                slotIn = calculateSlotIn();

                // remove from proposals
                removeProposal(m.getVote().getSlot());

                // broadcast decide
                // // System.out.println("Majority: " + acceptorsOfLogEntry + " A majority has accepted " + self + "'s proposal. Send decide messages. Log: " + log.toString() + " SlotIn/SlotOut (next not proposed/learn a decision before updating state): " + slotIn + "/" + slotOut);
                // print out deciisons and proposlas with the serverf name at the front
                // // System.out.println(self + " Decisions: " + decisions + " Proposals: " + proposals);
                broadcast(new Decide(m.getVote()));

                // execute the application until you encounter a slot that is not decided or a no op command
                executeCommand();

            }
        }
    }

    private void handlePrepare(Prepare m, int sender) {
        // Your code here...
        if (change_round(m.getRound())) {
            System.out.println("[SERVER " + serverId + "] Received Prepare from SERVER " + sender + " for round " + m.getRound());
            Prepared prepared = new Prepared(log, current_round);
            previousLeader = currentLeader; // save the previous leader
            currentLeader = sender; // update the current leader
            sendMessageToServer(sender, prepared);
        }
    }


    private void handlePrepared(Prepared m, int sender) {
        if (current_round == m.getRound() & !majority(prepared) &&
        leader_of(self, current_round)) {
                
            mergeLogs(log, m.getLog());
            prepared.add(sender);
            if (majority(prepared)) {
                executeCommand();
                // System.out.println("****** AT THIS POINT I AM NOW EXECUTING THE COMMAND *******");
                

                // loop through for slotOut to the end of the log
                // int indexOfSlotOut = indexOfSlotOut(slotOut);

                List<Propose> reproposeBatch = new ArrayList<Propose>();
                // // ADDED INSTABLE

                for (int i = slotOut; i < slotIn; i++) {
                    LogEntry logEntry = log.getLog().get(i);
                    if (logEntry != null &&
                            logEntry.getStatus() != PaxosLogSlotStatus.CHOSEN &&
                            !(logEntry.getCommand().getCommand() instanceof NoOp)) {
                        LogEntry entry = new LogEntry(i, current_round,
                                logEntry.getCommand(), PaxosLogSlotStatus.EMPTY);
                        Propose token = new Propose(entry);

                        reproposeBatch.add(token);
                    }
                }
                // add everthing in the proposals list to the repropose batch
                reproposeBatch.addAll(proposals);
                ProposeBatch batch = new ProposeBatch(reproposeBatch);
                if (reproposeBatch.size() > 0) {
                    send_proposal_batch(batch);
                }
            }
        }
    }

    private void handleDecide(Decide m, int sender) {
        // Your code here...
        if (!decided(m.getVote().getSlot())) {
            // Deal with LOG state
            LogEntry entry = m.getVote();
            LogEntry newEntry =
                    new LogEntry(entry.getSlot(), entry.getRound(), entry.getCommand(),
                            PaxosLogSlotStatus.CHOSEN);
            addToMap(log.getLog(), newEntry, newEntry.getSlot());
            slotIn = calculateSlotIn();
            // // System.out.println(self + " Deciding on " + m.vote() + " Log: " + log.toString() + " SlotIn/SlotOut (next not proposed/learn a decision before updating state): " + slotIn + "/" + slotOut);

            // remove from proposals
            removeProposal(m.getVote().getSlot());
            // execute the application until you encounter
            // a slot that is not decided or a no op command
            executeCommand();
        }
    }

    private void handleFirstUndecidedSlot(FirstUndecidedSlot m, int sender) {
        int node_slot_out = m.getSlot();
        int my_slot_out = slotOut;
       

        lastUndecidedSlots[sender] = node_slot_out;
        lastUndecidedSlots[self] = my_slot_out;

        if (node_slot_out < my_slot_out) {
            // send decide methods to the node that is behind
            int indexOfSlotOut = indexOfSlotOut(my_slot_out);
            int indexOfNodeSlotOut = indexOfSlotOut(node_slot_out);

            if (indexOfSlotOut == -1) {
                indexOfSlotOut = log.getLog().size();
            }

            // System.out.println(self + " says, " + sender + " is behind me. I will send a catchup decide. from (" + node_slot_out + ":" + indexOfNodeSlotOut + ") to (" + my_slot_out + ":" + indexOfSlotOut + ")");

            ArrayList<CatchupDecide> catchupDecides = new ArrayList<CatchupDecide>();

            for (int i = node_slot_out; i < my_slot_out; i++) {
                LogEntry logEntry = log.getLog().get(i);
                if (logEntry != null && logEntry.getStatus() == PaxosLogSlotStatus.CHOSEN) {
                    catchupDecides.add(new CatchupDecide(logEntry));
                }
            }

            /** BATCHING ***/
            BatchCatchupDecide batchCatchupDecide = new BatchCatchupDecide(catchupDecides);
            if (catchupDecides.size() > 0) {
                sendMessageToServer(sender, batchCatchupDecide);
            }
        }
    }

    private void handleBatchCatchupDecide(BatchCatchupDecide m, int sender) {
        for (CatchupDecide catchupDecide : m.getVotes()) {
            handleCatchupDecide(catchupDecide, sender);
        }
    }

    private void handleCatchupDecide(CatchupDecide m, int sender) {
        // Your code here...
        // forceful decide
        LogEntry logEntry = m.getVote();
        LogEntry puttingEntry = new LogEntry(logEntry.getSlot(), logEntry.getRound(),
                logEntry.getCommand(), PaxosLogSlotStatus.CHOSEN);
        addToMap(log.getLog(), puttingEntry, m.getVote().getSlot());
        slotIn = calculateSlotIn();
        // System.out.println("****** AT THIS POINT I AM NOW EXECUTING THE COMMAND IN CATCHUP *******");
        executeCommand();
    }



    //--------------//--------------// UTILITY //--------------//--------------//
    private void addToMap(SortedMap<Integer, LogEntry> log, LogEntry entry, int pos) {
        // add entry at position pos in the list
        // if the list is not that big add null commeands in the logentries
        // until that entry is reached in the last
        // if the list is bigger than pos, then replace the entry at pos
        // with the new entry
    
        int firstKey = log.size() == 0 ? 0 : log.firstKey();
        if (pos < firstKey || pos < garbageCollected) {
            return;
        }
    
        if (slotIn <= pos) {
        for (int i = slotIn; i < pos; i++) {
            NoOp noOp = new NoOp();
            AMOCommand amoNoOp = new AMOCommand(noOp, -1, -1);
            LogEntry noOpEntry = new LogEntry(i, entry.getRound(), amoNoOp,
            PaxosLogSlotStatus.EMPTY);
            log.put(i, noOpEntry);
        }
            log.put(pos, entry);
        } else {
            log.put(pos, entry);
        }
    }

    private int calculateSlotIn() {
        if (log.getLog().isEmpty()) {
            return slotIn;
        }
        int max = log.getLog().lastKey();
        return max + 1;
    }

    private int indexOfSlotOut(int slot) {
        // get the keys of the localLog
        SortedMap<Integer, LogEntry> localLog = log.getLog();
        ArrayList<Integer> keys = new ArrayList<Integer>(localLog.keySet());
        Collections.sort(keys);
        int indexOfSlotOut = keys.indexOf(slot);
        return indexOfSlotOut;
    }

    private void putInMultiMap(Multimap<LogEntry, Integer> multimap,
    LogEntry vote, Integer addy) {
        // get the values already bound to vote in multimap
        Collection<Integer> addresses = multimap.get(vote);
        // put addy in multimap only if it doesnt already exist
        if (!addresses.contains(addy)) {
            multimap.put(vote, addy);
        } else {

        }
    }

    // --------------//PAXOS UTLIITY//--------------//--------------//--------------//
    private LogEntry mergeEntries(LogEntry x, LogEntry y) {
        if (x == null || (x.getCommand().getCommand() instanceof NoOp)) {
            return y;
        }
        if (y == null || (y.getCommand().getCommand() instanceof NoOp)) {
            return x;
        }
        return (x.getRound() > y.getRound()) ? x : y;
    }

    private void mergeLogs(Log l1, Log l2) {
        // loop through all of l1 with indexes and key set
        ArrayList<Integer> keys = new ArrayList<Integer>(l1.getLog().keySet());
        Collections.sort(keys);
        for (int i = 0; i < keys.size(); i++) {
            int logSlot = keys.get(i);
            LogEntry logEntry = l1.getLog().get(logSlot);
            // get the same logentry from l2
            LogEntry logEntry2 = l2.getLog().get(logSlot);
            // l2 is not null
            if (logEntry2 != null) {
                LogEntry merged = mergeEntries(logEntry, logEntry2);
                addToMap(l1.getLog(), merged, logSlot);
                slotIn = calculateSlotIn();
            }
        }

        // add all entries from l2 that are not in l1
        // loop through the keys of l2
        for (Integer logSlot : l2.getLog().keySet()) {
            // if the logSlot is not in l1
            if (!l1.getLog().containsKey(logSlot)) {
                // add the logEntry to l1
                LogEntry logEntry = l2.getLog().get(logSlot);
                addToMap(l1.getLog(), logEntry, logSlot);
                slotIn = calculateSlotIn();
            }
        }

    }


    private boolean majority(HashSet<Integer> set) {
        return set.size() > serverAddresses.size() / 2;
    }

    private boolean majority(Collection<Integer> set) {
        return set.size() > serverAddresses.size() / 2;
    }

    private void sendProposalsAgain() {
        // loop through the proposals
        // send them again
        for (Propose p : proposals) {
            LogEntry thisEntry = p.getVote();
            thisEntry = new LogEntry(thisEntry.getSlot(), current_round,
                    thisEntry.getCommand(), thisEntry.getStatus());
            Propose sendingAgain = new Propose(thisEntry);
            broadcast(sendingAgain);
        }
    }

    private void send_proposal_batch(ProposeBatch batch) {
        List<Propose> votes = batch.getVotes();
        // put each log entry accordingly in the multimap
        for (Propose vote : votes) {
            LogEntry logVote = vote.getVote();
            LogEntry logEntry = new LogEntry(logVote.getSlot(), current_round,
                    logVote.getCommand(), PaxosLogSlotStatus.EMPTY);
            // add to acceptors
            putInMultiMap(acceptorsOfLogEntry, logEntry, self);
            // add to proposals
            proposals.add(vote);
            // add to log
            // addToMap(log.log(), logVote, logVote.slot());
            slotIn = calculateSlotIn();
        }
        broadcast(batch);
        //handleProposeBatch(batch, self);
    }

    private void broadcast(MessageBody m) {
        // loop through array servers

        // Nodes should never send messages to themselves.
        for (String address : serverAddresses) {
            String[] parts = address.split(":");
            String ip = parts[0];
            int port = Integer.parseInt(parts[1]);
            if (ip.equals(myIp) && port == myListenPort) {
                continue; // Skip sending message to self
            }

            Message message = new Message(myIp, myListenPort, ip, port, m);
            // System.out.println("[SERVER " + serverId + "] Broadcasting message " + m  + " to " + ip + ":" + port + " with id " + serverIpToId.get(ip + ":" + port));
            sendMessage(message);
        }
    }

    private boolean existsInProposals(Command command) {
        assert (command instanceof AMOCommand);
        for (Propose p : proposals) {
            if (p.getVote().getCommand().equals(command)) {
                return true;
            }
        }
        return false;
    }

    private LogEntry entryAccepted(AMOCommand value) {
        return new LogEntry(slotIn, current_round, value,
                PaxosLogSlotStatus.ACCEPTED);
    }

    private void send_proposal() {
        assert (requests.size() > 0);


        // deal with requests set
        AMOCommand command = (AMOCommand) requests.iterator().next();
        requests.remove(command);

        LogEntry logVote = entryAccepted(command);

        // deal with proposals set
        Propose myPropose = new Propose(logVote);
        proposals.add(myPropose);

        // deal with log entry creation
        // print slotIn
        addToMap(log.getLog(), logVote, slotIn);
        slotIn = calculateSlotIn();

        // add to acceptors
        putInMultiMap(acceptorsOfLogEntry, logVote, self);

        // execute broadcast
        broadcast(myPropose);
        // handlePropose(myPropose, self);
        // // System.out.println("Proposing: " + logVote + " Log: " + log.toString() + " My Acceptance: " + acceptorsOfLogEntry);
    }

    public PaxosLogSlotStatus status(int logSlotNum) {
        // Your code here...
        if (log.getLog() == null) {
            return PaxosLogSlotStatus.EMPTY;
        }
        if (log.getLog().isEmpty() && logSlotNum < slotIn) {
            return PaxosLogSlotStatus.CLEARED;
        }
        int firstKey = log.getLog().size() == 0 ? 0 : log.getLog().firstKey();
        if (logSlotNum < firstKey) {
            return PaxosLogSlotStatus.CLEARED;
        }

        LogEntry entry = log.getLog().get(logSlotNum);
        if (entry == null) {
            return PaxosLogSlotStatus.EMPTY;
        }
        return entry.getStatus();
    }

    private boolean decided(int slot) {
        return log != null && status(slot) == PaxosLogSlotStatus.CHOSEN;
    }

    private void removeProposal(int slot) {
        // loop through the proposals
        // remove the proposal with the slot
        // send the rest again
        Propose toRemove = null;
        for (Propose p : proposals) {
            if (p.getVote().getSlot() == slot) {
                toRemove = p;
                break;
            }
        }
        if (toRemove != null) {
            proposals.remove(toRemove);
        }
    }

    private void writeUpdateToFile() {
                // print out the application to server_id_app.txt
        // append to the file, the first line is Time: time
        // the second line is the application state
        // the third line is the log
        String filename = "server_" + serverId + "_app.txt";
        try {
            FileWriter fw = new FileWriter(filename, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("Time: " + this.time + "\n");
            bw.write(app.toString() + "\n");
            bw.write(log.toString() + "\n");
            bw.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to write to file: " + e.getMessage());
        }
    }

    // write messages to file
    private void writeMessageToFile(String message) {
        // append to the file, the first line is Time: time
        // the second line is the message
        String filename = "server_" + serverId + "_messages.txt";
        try {
            FileWriter fw = new FileWriter(filename, true);
            BufferedWriter bw = new BufferedWriter(fw);
            // Messages received by node 1 from the previous leader (PL), new leader (NL), or another follower (F)
            bw.write(getExactTime() + message + "\n");
            bw.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to write to file: " + e.getMessage());
        }
    }

    // method to get exact time 
    private String getExactTime() {
        // Define the format HH:MM:SS:MM
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SS");
        // Get the current date and time
        Date now = new Date();
        // Format the date to a string
        return '[' + formatter.format(now) + "] "; // Return the formatted time string
    }



}

