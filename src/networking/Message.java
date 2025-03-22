package networking;

import java.io.Serializable;

/**
 * Represents a message in the distributed system with information about
 * sender, receiver, and the message body.
 */
public class Message implements Serializable {
    private final String senderIp;
    private final int senderPort;
    private final String receiverIp;
    private final int receiverPort;
    private final String messageBody;
    private final MessageType type;

    public enum MessageType {
        REQUEST,
        REPLY,
        HEARTBEAT
    }

    public Message(String senderIp, int senderPort, String receiverIp, int receiverPort,
                   String messageBody, MessageType type) {
        this.senderIp = senderIp;
        this.senderPort = senderPort;
        this.receiverIp = receiverIp;
        this.receiverPort = receiverPort;
        this.messageBody = messageBody;
        this.type = type;
    }

    public String getSenderIp() {
        return senderIp;
    }

    public int getSenderPort() {
        return senderPort;
    }

    public String getReceiverIp() {
        return receiverIp;
    }

    public int getReceiverPort() {
        return receiverPort;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public MessageType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type + ": " + messageBody +
                " Sender: " + senderIp + ":" + senderPort +
                " Receiver: " + receiverIp + ":" + receiverPort;
    }

    /**
     * Create a message from a string representation
     */
    public static Message fromString(String messageStr) {
        String[] parts = messageStr.split(" ");

        MessageType type = parts[0].equals("Request:") ? MessageType.REQUEST : MessageType.REPLY;

        // Extract sender info
        String[] senderInfo = parts[9].split(":");
        String senderIp = senderInfo[0];
        int senderPort = Integer.parseInt(senderInfo[1]);

        // Extract receiver info
        String[] receiverInfo = parts[11].split(":");
        String receiverIp = receiverInfo[0];
        int receiverPort = Integer.parseInt(receiverInfo[1]);

        // Extract message body (everything between type and sender)
        StringBuilder bodyBuilder = new StringBuilder();
        for (int i = 1; i < 9; i++) {
            bodyBuilder.append(parts[i]).append(" ");
        }
        String messageBody = bodyBuilder.toString().trim();

        return new Message(senderIp, senderPort, receiverIp, receiverPort, messageBody, type);
    }
}