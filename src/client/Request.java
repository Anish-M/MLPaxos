package client;

import commands.Command;
import java.io.Serializable;

import commands.Command;
import networking.MessageBody;

/**
 * Updated Request class with support for network information
 */
public class Request extends MessageBody implements Serializable {
    private final Command command;
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


    public long getRequestId() {
        return requestId;
    }

    public String toString() {
        return "Request: " + command.toString();
    }
}