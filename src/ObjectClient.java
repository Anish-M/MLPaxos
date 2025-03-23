import java.io.*;
import java.net.*;


// Client class
class ObjectClient {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 6023;
        try (Socket socket = new Socket(host, port)) {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            CustomObject obj = new CustomObject("Hello, Server!", 42);
            oos.writeObject(obj);
            System.out.println("Sent: " + obj);

            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}