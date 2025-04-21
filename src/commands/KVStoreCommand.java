package commands;

import application.Application;
import results.Result;

public abstract class KVStoreCommand implements Command {
    protected String key;

    public KVStoreCommand() {
    }
    public KVStoreCommand(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public abstract Result execute(Application app);
}
