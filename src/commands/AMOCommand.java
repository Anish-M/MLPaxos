package commands;

import results.Result;
import application.Application;

// AMOCommand.java
public class AMOCommand implements Command {
    private final Command command;
    private final long clientId;
    private final long requestId;

    public AMOCommand(Command command, long clientId, long requestId) {
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

    @Override
    public Result execute(Application app) {
        return app.execute(this);
    }

    @Override
    public String toString() {
        return "AMOCommand: " + command.toString();
    }
}