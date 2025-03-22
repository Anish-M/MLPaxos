package commands;

import application.Application;
import results.Result;

public class PutCommand extends KVStoreCommand {
    private String value;

    public PutCommand(String key, String value) {
        super(key);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public Result execute(Application app) {
        return app.execute(this);
    }

    @Override
    public String toString() {
        return "PutCommand: Key: " + getKey() + " Value: " + value;
    }
}