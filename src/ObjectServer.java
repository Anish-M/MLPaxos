import java.io.*;
import java.net.*;

// Custom object to be sent over sockets
class CustomObject implements Serializable {
    private static final long serialVersionUID = 1L;
    private String message;
    private int value;

    public CustomObject(String message, int value) {
        this.message = message;
        this.value = value;
    }

    @Override
    public String toString() {
        return "CustomObject{message='" + message + "', value=" + value + "}";
    }
}

// Server class
class ObjectServer {
    public static void main(String[] args) {
        int port = 6023;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);
            Socket socket = serverSocket.accept();
            System.out.println("Client connected");

            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            CustomObject receivedObject = (CustomObject) ois.readObject();
            System.out.println("Received: " + receivedObject);

            ois.close();
            socket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
