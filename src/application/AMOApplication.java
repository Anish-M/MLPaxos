package application;

// AMOApplication.java
import commands.AMOCommand;
import results.AMOResult;
import commands.Command;
import results.Result;

import java.util.HashMap;
import java.util.Map;

public class AMOApplication implements Application {
    private final Application app;
    private final Map<Long, Map<Long, Result>> clientHistory;

    public AMOApplication(Application app) {
        this.app = app;
        this.clientHistory = new HashMap<>();
    }

    @Override
    public Result execute(Command cmd) {
        if (!(cmd instanceof AMOCommand)) {
            throw new IllegalArgumentException("AMOApplication only accepts AMOCommands");
        }

        AMOCommand amoCmd = (AMOCommand) cmd;
        long clientId = amoCmd.getClientId();
        long requestId = amoCmd.getRequestId();

        // Check if we've already executed this command
        if (clientHistory.containsKey(clientId)) {
            Map<Long, Result> clientRequests = clientHistory.get(clientId);
            if (clientRequests.containsKey(requestId)) {
                // We've already executed this command, return the cached result
                return new AMOResult(clientRequests.get(requestId), requestId);
            }
        }

        // Execute the command
        Result result = app.execute(amoCmd.getCommand());

        // Cache the result
        clientHistory.putIfAbsent(clientId, new HashMap<>());
        clientHistory.get(clientId).put(requestId, result);

        return new AMOResult(result, requestId);
    }

    public Result executeReadOnly(Command cmd) {
        // For non-AMO read-only commands
        return app.execute(cmd);
    }
}