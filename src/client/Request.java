package client;

import commands.Command;
import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Command command;

    private final long clientId;
    private final long requestId;

    public Request(Command command) {
        this.command = command;
        if (command instanceof commands.AMOCommand) {
            this.clientId = ((commands.AMOCommand) command).getClientId();
            this.requestId = ((commands.AMOCommand) command).getRequestId();
        } else {
            this.clientId = -1;
            this.requestId = -1;
        }
    }

    public Request(Command command, long clientId, long requestId) {
        this.command = command;
        this.clientId = clientId;
        this.requestId = requestId;
    }

    public Command getCommand() {
        return command;
    }

    public long getClientId() {
        return clientId;
    }

    public long getRequestId() {
        return requestId;
    }

    public String socketRequest() {
        String commandStr = command.toString();
        return "Request: " + commandStr + " Client ID: " + clientId + " Request ID: " + requestId;
    }

}