package commands;

import application.Application;
import results.Result;

public class AppendCommand extends KVStoreCommand {
    private String value;

    public AppendCommand(String key, String value) {
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
        return "AppendCommand: Key: " + getKey() + " Value: " + value;
    }
}