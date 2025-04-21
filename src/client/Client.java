package client;

import commands.Command;
import commands.*;
import networking.Heartbeat;
import networking.Message;
import networking.MessageBody;
import results.AMOResult;
import results.Result;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Client {
    private final String clientIp;
    private final int clientPort;
    private ServerSocket clientSocket;

    private HashMap<Long, Result> commandIdToResult;

    private ArrayList<String> serverAddressesIpPort;

    // data structures to hold server and client addresses
    private ArrayList<String> serverAddresses;
    private ArrayList<String> clientAddresses;
    private Map<String, Integer> serverIpToId;
    private Map<Integer, String> serverIdToIp;

    private static int requestId;

    private String myIp;
    private int myPort;
    private int clientId;

    public Client(ArrayList<String> serverAddressesIpPort, int clientPort, ArrayList<String> serverAddresses, ArrayList<String> clientAddresses, Map<String, Integer> serverIpToId, Map<Integer, String> serverIdToIp, int clientID) throws IOException {
        // Dynamically allocate a port for the client
        ServerSocket tempSocket = new ServerSocket(clientPort);
        this.clientPort = tempSocket.getLocalPort();
        tempSocket.close();
        commandIdToResult = new HashMap<>();
        this.serverAddressesIpPort = serverAddressesIpPort;
        requestId = 0;
        myIp = Inet4Address.getLocalHost().getHostAddress();
        myPort = clientPort;
        this.clientIp = myIp;
        this.serverAddresses = serverAddresses;
        this.clientAddresses = clientAddresses;
        this.serverIpToId = serverIpToId;
        this.serverIdToIp = serverIdToIp;
        this.clientId = clientID;
    }

    public void startClient() {
        try {
            clientSocket = new ServerSocket(clientPort);
            System.out.println("[CLIENT] Listening for messages on port " + clientPort);

            // Start a thread to receive messages
            new Thread(this::receiveMessage).start();

            // create another thread that uses scanner to send messages to the server
            new Thread(this::sendMessagesScanner).start();
        } catch (IOException e) {
            System.err.println("[CLIENT] Error starting client: " + e.getMessage());
        }
    }

    public void sendMessagesScanner() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Enter the server, command, and arguments in the format: serverId command args");
            String command = scanner.nextLine();
            if (command.equals("exit")) {
                break;
            }

            // do safety chekces must be three space separated strings
            if (command.split(" ").length < 3) {
                System.out.println("Invalid command \n");
                continue;
            }

            String[] parts = command.split(" ");
            int serverId = Integer.parseInt(parts[0]);
            String commandStr = parts[1];
            String[] args = Arrays.copyOfRange(parts, 2, parts.length);
            
            // AppendCommand, GetCommand, PutCommand, NoOp
            if (commandStr.equals("AppendCommand")) {
                AppendCommand appendCommand = new AppendCommand(args[0], args[1]);
                AMOCommand amoAppendCommand = new AMOCommand(appendCommand, clientId, requestId);
                sendMessageToServer(serverId, amoAppendCommand);
            } else if (commandStr.equals("GetCommand")) {
                GetCommand getCommand = new GetCommand(args[0]);
                AMOCommand amoGetCommand = new AMOCommand(getCommand, clientId, requestId);
                sendMessageToServer(serverId, amoGetCommand);
            } else if (commandStr.equals("PutCommand")) {
                PutCommand putCommand = new PutCommand(args[0], args[1]);
                AMOCommand amoPutCommand = new AMOCommand(putCommand, clientId, requestId);
                sendMessageToServer(serverId, amoPutCommand);
            } else {
                System.out.println("Invalid command");
            }

    
        }
    }

    public void sendMessageToServer(int serverId, MessageBody messageBody) {
        String serverIp = serverIdToIp.get(serverId);
        int serverPort = Integer.parseInt(serverAddresses.get(serverId).split(":")[1]);
        String serverIpOnly = serverIp.split(":")[0];
        sendMessage(serverIpOnly, serverPort, messageBody);
        requestId++;
    }

    public void sendMessage(String serverIp, int serverPort, MessageBody messageBody) {
        try (Socket socket = new Socket(serverIp, serverPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            Message message = new Message(clientIp, clientPort, serverIp, serverPort, messageBody);
            out.writeObject(message);
            out.flush();
            int serverId = serverIpToId.get(serverIp + ":" + serverPort);
            System.out.println("[CLIENT] Sent " + messageBody.toString() + " to SERVER " + serverId);

        } catch (IOException e) {
            int serverId = serverIpToId.get(serverIp);
            System.err.println("[CLIENT] Error sending message to SERVER " + serverId + ": " + e);
        }
    }

    public void receiveMessage() {
        while (true) {
            try (Socket socket = clientSocket.accept();
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                Message message = (Message) in.readObject();
                processReceivedMessage(message);

            } catch (IOException | ClassNotFoundException e) {
                int serverId = serverIpToId.get(myIp);
                System.err.println("[CLIENT] Error receiving message: " + serverId);
            }
        }
    }

    private void processReceivedMessage(Message message) {
        String senderIpPort = message.getSenderIp() + ":" + message.getSenderPort();
        if (message.getMessageBody() instanceof Reply) {
            AMOResult amoResult = (AMOResult) ((Reply) message.getMessageBody()).getResult();
            Result result = amoResult.getResult();
            commandIdToResult.put(amoResult.getRequestId(), result);
            System.out.println("[CLIENT] Received REPLY from SERVER " + senderIpPort + ": " + result);
        } else {
            System.out.println("[CLIENT] Received unknown message type");
        }
    }

    public static void main(String[] args) throws IOException {
            String myCurrentIpAddr = Inet4Address.getLocalHost().getHostAddress();
            int myPort = -1;

            ArrayList<String> serverAddresses = new ArrayList<>();
            ArrayList<String> clientAddresses = new ArrayList<>();
            Map<String, Integer> serverIpToId = new HashMap<>();
            Map<Integer, String> serverIdToIp = new HashMap<>();
            int clientId = -1;


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
                serverIpToId.put(ipPortStr, Integer.parseInt(ipPortId[1]));
                serverIdToIp.put(Integer.parseInt(ipPortId[1]), ipPortStr);

            }

            assert myPort != -1 : "Server port not found for IP: " + myCurrentIpAddr;

            line = br.readLine();
            System.out.println("[INFO] Reading client configuration from file: " + line);
            String[] clientParts = line.split(" ");
            int numberOfClients = Integer.parseInt(clientParts[1]);
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
            }

            // print out both the server and client addresses
            System.out.println("[INFO] Server Addresses: " + serverIdToIp);
            System.out.println("[INFO] Client Addresses: " + clientAddresses);

            // close the file reader
            br.close();


            // Create the Client
            Client client = new Client(serverAddresses, myPort, serverAddresses, clientAddresses, serverIpToId, serverIdToIp, clientId);
            client.startClient();
    }
}
