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
        this.scheduler = Executors.newScheduledThreadPool(1);
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

    public void shutdown() {
        scheduler.shutdown();
    }
}