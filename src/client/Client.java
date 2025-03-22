package client;
// Client.java
import application.Application;
import client.ClientTimer;
import client.Reply;
import client.Request;
import commands.AMOCommand;
import commands.Command;
import results.*;
import server.Server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

public class Client {
    private final long clientId;
    private final AtomicLong requestIdCounter;
    private final ScheduledExecutorService scheduler;
    private final long timeoutMs;
    private final Server server;

    public Client(long clientId, Server server, long timeoutMs) {
        this.clientId = clientId;
        this.requestIdCounter = new AtomicLong(0);
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.timeoutMs = timeoutMs;
        this.server = server;
    }

    public Result execute(Command cmd) {
        long requestId = requestIdCounter.getAndIncrement();
        AMOCommand amoCmd = new AMOCommand(cmd, clientId, requestId);
        Request request = new Request(amoCmd);

        // Create and start a timer for this request
        ClientTimer timer = new ClientTimer(scheduler, () -> {
            // Retry the request if timeout occurs
            server.handleRequest(request);
        }, timeoutMs);
        timer.start();

        // Send the request to the server
        Reply reply = server.handleRequest(request);

        // Cancel the timer as we got a response
        timer.cancel();

        if (reply.getResult() instanceof AMOResult) {
            AMOResult amoResult = (AMOResult) reply.getResult();
            return amoResult.getResult();
        } else {
            throw new RuntimeException("Expected AMOResult but got: " + reply.getResult().getClass().getName());
        }
    }

    public Reply socketReplyToObj(String socketReply) {
        // parse this "Reply: " + resultStr + " Server ID: " + serverId + " Request ID: " + requestId;
        String[] parts = socketReply.split(" ");
        String resultStr = parts[1];
        // parse something like this return "AMOResult: " + "PutOk: " + "Key: " + key + " Value: " + value;
        String[] resultParts = resultStr.split(" ");
        String resultName = resultParts[0];
        Result result = null;
        if (resultName.equals("PutOk:")) {
            String key = resultParts[2];
            String value = resultParts[4];
            result = new PutOk();
        } else if (resultName.equals("GetResult:")) {
            String value = resultParts[2];
            result = new GetResult(value);
        } else if (resultName.equals("KeyNotFound:")) {
            String key = resultParts[2];
            result = new KeyNotFound(key);
        } else if (resultName.equals("AppendResult:")) {
            String newValue = resultParts[2];
            result = new AppendResult(newValue);
        } else {
            throw new RuntimeException("Unknown result: " + resultName);
        }
        long serverId = Long.parseLong(parts[4]);
        long requestId = Long.parseLong(parts[7]);
        return new Reply(result, serverId, requestId);
    }
    public void shutdown() {
        scheduler.shutdown();
    }
}
