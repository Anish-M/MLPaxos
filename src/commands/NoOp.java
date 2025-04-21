package commands;

import application.Application;
import results.Result;

public class NoOp extends KVStoreCommand {
    public NoOp() {
        super();
    }

    @Override
    public Result execute(Application app) {
        return null;
    }

    @Override
    public String toString() {
        return "NoOp: ";
    }
}
