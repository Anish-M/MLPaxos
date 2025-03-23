package client;

import networking.Heartbeat;
import networking.Message;
import networking.MessageBody;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private final String clientIp = "127.0.0.1";
    private final int clientPort;
    private ServerSocket clientSocket;

    public Client() throws IOException {
        // Dynamically allocate a port for the client
        ServerSocket tempSocket = new ServerSocket(0);
        this.clientPort = tempSocket.getLocalPort();
        tempSocket.close();
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
        if (message.getMessageBody() instanceof Heartbeat) {
            System.out.println("[CLIENT] Received HEARTBEAT from SERVER " + message.getSenderIp() + ":" + message.getSenderPort());
        } else {
            System.out.println("[CLIENT] Received unknown message type");
        }
    }

    public void sendHeartbeat(String serverIp, int serverPort) {
        sendMessage(serverIp, serverPort, new Heartbeat());
    }

    public static void main(String[] args) {
        try {
            Client client = new Client();
            client.startClient();

            Scanner scanner = new Scanner(System.in);

            // Allow user to manually send heartbeats
            while (true) {
                System.out.print("[CLIENT] Enter server IP and port (format: 127.0.0.1:5000) to send heartbeat, or type 'exit': ");
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("exit")) {
                    break;
                }

                String[] parts = input.split(":");
                if (parts.length == 2) {
                    String serverIp = parts[0];
                    int serverPort = Integer.parseInt(parts[1]);
                    client.sendHeartbeat(serverIp, serverPort);
                } else {
                    System.out.println("[CLIENT] Invalid format. Use 127.0.0.1:port.");
                }
            }

            scanner.close();
        } catch (IOException e) {
            System.err.println("[CLIENT] Failed to start: " + e.getMessage());
        }
    }
}
