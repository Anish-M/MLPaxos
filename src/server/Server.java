package server;

import application.AMOApplication;
import application.Application;
import application.KVStore;
import client.Reply;
import client.Request;
import networking.Heartbeat;
import networking.Message;
import results.Result;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private final Application app;
    private final ArrayList<String> serverAddresses;

    private final ArrayList<String> clientAddresses;

    // ip addr to server id
    private final Map<String, Integer> serverIpToId;

    private final int myListenPort;
    private final int serverId;
    private ServerSocket serverSocket;

    private final String myIp = "127.0.0.1";

    public Server(Application app, ArrayList<String> serverAddresses, ArrayList<String> clientAddresses, int myListenPort, Map<String, Integer> serverIpToId) {
        this.app = app;
        this.serverAddresses = serverAddresses;
        this.clientAddresses = clientAddresses;
        this.myListenPort = myListenPort;
        this.serverId = serverIpToId.get(myIp + ":" + myListenPort);
        this.serverIpToId = serverIpToId;
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(myListenPort);
            System.out.println("[SERVER " + serverId + "] Listening on port " + myListenPort);

            new Thread(this::startHeartbeat).start();
            new Thread(this::listenForMessages).start();
        } catch (IOException e) {
            System.err.println("[SERVER " + serverId + "] Error starting server: " + e.getMessage());
        }
    }
    private void startHeartbeat() {
        try {
            Thread.sleep(3000); // Ensure all servers start before heartbeats begin
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        while (true) {
            for (String address : serverAddresses) {
                String[] parts = address.split(":");
                String ip = parts[0];
                int port = Integer.parseInt(parts[1]);

                Message heartbeatMessage = new Message("127.0.0.1", myListenPort, ip, port, new Heartbeat());
                sendMessage(heartbeatMessage);
            }
            try {
                Thread.sleep(5000); // Heartbeat interval
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void sendMessage(Message message) {
        try (Socket socket = new Socket(message.getReceiverIp(), message.getReceiverPort());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            out.writeObject(message);
            out.flush();

            String receiverIpPort = message.getReceiverIp() + ":" + message.getReceiverPort();
            System.out.println("[SERVER " + serverId + "] Sent MESSAGE to SERVER " + serverIpToId.get(receiverIpPort));

        } catch (IOException e) {
            String receiverIpPort = message.getReceiverIp() + ":" + message.getReceiverPort();
            System.err.println("[SERVER " + serverId + "] Error sending message to SERVER " + serverIpToId.get(receiverIpPort));
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
            System.err.println("[SERVER " + serverId + "] Error processing message: " + e.getMessage());
        }
    }

    private void processReceivedMessage(Message message) {
        // message from client
        String senderIpPort = message.getSenderIp() + ":" + message.getSenderPort();
        if (clientAddresses.contains(senderIpPort)){
            processReceivedMessageFromClient(message);
            return;
        }

        if (message.getMessageBody() instanceof Heartbeat) {
            System.out.println("[SERVER " + serverId + "] Received HEARTBEAT from SERVER " + serverIpToId.get(senderIpPort));
        } else {
            System.out.println("[SERVER " + serverId + "] Received unknown message type");
        }
    }

    private void processReceivedMessageFromClient(Message message) {
        Request request = (Request) message.getMessageBody();
        Result result = app.execute(request.getCommand());
        Reply reply = new Reply(result);
        Message replyMessage = new Message(myIp, myListenPort, message.getSenderIp(), message.getSenderPort(), reply);
        sendMessage(replyMessage);
    }


    public static void main(String[] args) {
        try {
            // Create servers with dynamically assigned ports
            ServerSocket tempSocket1 = new ServerSocket(0);
            int port1 = tempSocket1.getLocalPort();
            tempSocket1.close();

            ServerSocket tempSocket2 = new ServerSocket(0);
            int port2 = tempSocket2.getLocalPort();
            tempSocket2.close();

            ServerSocket tempSocket3 = new ServerSocket(0);
            int port3 = tempSocket3.getLocalPort();
            tempSocket3.close();

            ArrayList<String> server1List = new ArrayList<>(List.of("127.0.0.1:" + port2, "127.0.0.1:" + port3));
            ArrayList<String> server2List = new ArrayList<>(List.of("127.0.0.1:" + port1, "127.0.0.1:" + port3));
            ArrayList<String> server3List = new ArrayList<>(List.of("127.0.0.1:" + port1, "127.0.0.1:" + port2));

            Map<String, Integer> serverIpToId = new HashMap<>();
            serverIpToId.put("127.0.0.1:" + port1, 1);
            serverIpToId.put("127.0.0.1:" + port2, 2);
            serverIpToId.put("127.0.0.1:" + port3, 3);

            // write out this map to a file so that the client can read it
            try {
                FileOutputStream fileOut = new FileOutputStream("serverIpToId.txt");
                ObjectOutputStream out = new ObjectOutputStream(fileOut);
                for (Map.Entry<String, Integer> entry : serverIpToId.entrySet()) {
                    out.writeObject(entry.getKey());
                    out.writeObject(entry.getValue());
                }
                out.close();
                fileOut.close();
                System.out.println("Serialized data is saved in serverIpToId.ser");
            } catch (IOException i) {
                i.printStackTrace();
            }


            Server server1 = new Server(new AMOApplication(new KVStore()), server1List, new ArrayList<>(), port1, serverIpToId);
            Server server2 = new Server(new AMOApplication(new KVStore()), server2List, new ArrayList<>(), port2, serverIpToId);
            Server server3 = new Server(new AMOApplication(new KVStore()), server3List, new ArrayList<>(), port3, serverIpToId);

            new Thread(server1::startServer).start();
            new Thread(server2::startServer).start();
            new Thread(server3::startServer).start();

            System.out.println("[INFO] Servers started on dynamic ports: " + port1 + ", " + port2 + ", " + port3);
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to allocate ports: " + e.getMessage());
        }
    }
}