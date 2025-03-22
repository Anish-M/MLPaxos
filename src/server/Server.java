package server;

import application.Application;
import client.Reply;
import client.Request;
import commands.*;
import results.Result;

// Server.java
public class Server {

    public static int serverIdCounter = 0;

    private final int serverId;

    private final Application app;

    public Server(Application app) {
        this.app = app;
        this.serverId = serverIdCounter++;
    }

    public Reply handleRequest(Request request) {
        Result result = app.execute(request.getCommand());
        return new Reply(result, serverId, request.getRequestId());
    }

    public Request socketRequestToObj(String socketRequest) {
        // parse this "Request: " + commandStr + " Client ID: " + clientId + " Request ID: " + requestId;
        String[] parts = socketRequest.split(" ");
        String commandStr = parts[1];
        // parse something like this return "AMOCommand: " + "Request: " + "PutCommand: Key: " + getKey() + " Value: " + value + " Client ID: " + clientId + " Request ID: " + requestId;
        String[] commandParts = commandStr.split(" ");
        String commandName = commandParts[0];
        Command command = null;
        if (commandName.equals("PutCommand:")) {
            String key = commandParts[2];
            String value = commandParts[4];
            command = new PutCommand(key, value);
        } else if (commandName.equals("GetCommand:")) {
            String key = commandParts[2];
            command = new GetCommand(key);
        } else if (commandName.equals("AppendCommand:")) {
            String key = commandParts[2];
            String value = commandParts[4];
            command = new AppendCommand(key, value);
        } else {
            throw new RuntimeException("Unknown command: " + commandName);
        }
        long clientId = Long.parseLong(parts[4]);
        long requestId = Long.parseLong(parts[7]);
        AMOCommand amoCommand = new AMOCommand(command, clientId, requestId);
        return new Request(command);
    }
}