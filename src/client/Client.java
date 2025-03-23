package client;

import commands.Command;
import networking.Heartbeat;
import networking.Message;
import networking.MessageBody;
import results.AMOResult;
import results.Result;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Client {
    private final String clientIp = "127.0.0.1";
    private final int clientPort;
    private ServerSocket clientSocket;

    private HashMap<Long, Result> commandIdToResult;

    private ArrayList<String> serverAddressesIpPort;

    public Client() throws IOException {
        // Dynamically allocate a port for the client
        ServerSocket tempSocket = new ServerSocket(0);
        this.clientPort = tempSocket.getLocalPort();
        tempSocket.close();
        commandIdToResult = new HashMap<>();
    }

    public void startClient() {
        try {
            clientSocket = new ServerSocket(clientPort);
            System.out.println("[CLIENT] Listening for messages on port " + clientPort);

            // Start a thread to receive messages
            new Thread(this::receiveMessage).start();
        } catch (IOException e) {
            System.err.println("[CLIENT] Error starting client: " + e.getMessage());
        }
    }

    public void broadcastMessage(MessageBody messageBody) {
        for (String serverAddress : serverAddressesIpPort) {
            String[] parts = serverAddress.split(":");
            String serverIp = parts[0];
            int serverPort = Integer.parseInt(parts[1]);
            sendMessage(serverIp, serverPort, messageBody);
        }
    }

    public void sendMessage(String serverIp, int serverPort, MessageBody messageBody) {
        try (Socket socket = new Socket(serverIp, serverPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            Message message = new Message(clientIp, clientPort, serverIp, serverPort, messageBody);
            out.writeObject(message);
            out.flush();

            System.out.println("[CLIENT] Sent MESSAGE to SERVER " + serverIp + ":" + serverPort);

        } catch (IOException e) {
            System.err.println("[CLIENT] Error sending message to SERVER " + serverIp + ":" + serverPort + ": " + e.getMessage());
        }
    }

    public void receiveMessage() {
        while (true) {
            try (Socket socket = clientSocket.accept();
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                Message message = (Message) in.readObject();
                processReceivedMessage(message);

            } catch (IOException | ClassNotFoundException e) {
                System.err.println("[CLIENT] Error receiving message: " + e.getMessage());
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

    public static void main(String[] args) {
        try {
            Client client = new Client();
            client.startClient();

            Scanner scanner = new Scanner(System.in);

            // Allow user to manually send heartbeats


            scanner.close();
        } catch (IOException e) {
            System.err.println("[CLIENT] Failed to start: " + e.getMessage());
        }
    }
}
