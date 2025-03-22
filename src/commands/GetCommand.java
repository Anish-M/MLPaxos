package commands;

import application.Application;
import results.Result;

public class GetCommand extends KVStoreCommand {
    public GetCommand(String key) {
        super(key);
    }

    @Override
    public Result execute(Application app) {
        return app.execute(this);
    }

    @Override
    public String toString() {
        return "GetCommand: Key: " + getKey();
    }

}