package client;

import commands.Command;
import java.io.Serializable;

import commands.Command;

/**
 * Updated Request class with support for network information
 */
public class Request {
    private final Command command;
    private String clientIp;
    private int clientPort;
    private long requestId;

    public Request(Command command) {
        this.command = command;
        if (command instanceof commands.AMOCommand) {
            commands.AMOCommand amoCommand = (commands.AMOCommand) command;
            this.requestId = amoCommand.getRequestId();
        }
    }

    public Command getCommand() {
        return command;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public long getRequestId() {
        return requestId;
    }
}