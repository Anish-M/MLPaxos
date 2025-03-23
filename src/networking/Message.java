package networking;

import client.Reply;
import client.Request;

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
    private final MessageBody messageBody;

    private MessageType type;

    public enum MessageType {
        REQUEST,
        REPLY,
        HEARTBEAT
    }

    public Message(String senderIp, int senderPort, String receiverIp, int receiverPort,
                   MessageBody messageBody) {
        this.senderIp = senderIp;
        this.senderPort = senderPort;
        this.receiverIp = receiverIp;
        this.receiverPort = receiverPort;
        this.messageBody = messageBody;

        if (messageBody instanceof Request) {
            this.type = MessageType.REQUEST;
        } else if (messageBody instanceof Reply) {
            this.type = MessageType.REPLY;
        } else {
            this.type = MessageType.HEARTBEAT;
        }

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

    public MessageBody getMessageBody() {
        return messageBody;
    }

    public MessageType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type + ": " + messageBody +
                " Sender: " + senderIp +
                " Receiver: " + receiverIp + ":" + receiverPort;
    }

}