package server;

import application.Application;
import client.Reply;
import client.Request;
import commands.*;
import results.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import networking.*;

public class Server {
    private final Application app;

    public Server(Application app) {
        this.app = app;
    }

    public Reply handleRequest(Request request) {
        Result result = app.execute(request.getCommand());
        return new Reply(result);
    }
}