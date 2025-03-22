package application;

import results.*;
import commands.*;

import java.util.HashMap;
import java.util.Map;

public class KVStore implements Application {
    private Map<String, String> store;

    public KVStore() {
        store = new HashMap<>();
    }

    @Override
    public Result execute(Command cmd) {
        if (cmd instanceof GetCommand) {
            return executeGet((GetCommand) cmd);
        } else if (cmd instanceof PutCommand) {
            return executePut((PutCommand) cmd);
        } else if (cmd instanceof AppendCommand) {
            return executeAppend((AppendCommand) cmd);
        } else {
            throw new IllegalArgumentException("Unknown command type: " + cmd.getClass().getName());
        }
    }

    private Result executeGet(GetCommand cmd) {
        String key = cmd.getKey();
        if (store.containsKey(key)) {
            return new GetResult(store.get(key));
        } else {
            return new KeyNotFound(key);
        }
    }

    private Result executePut(PutCommand cmd) {
        String key = cmd.getKey();
        String value = cmd.getValue();
        store.put(key, value);
        return new PutOk();
    }

    private Result executeAppend(AppendCommand cmd) {
        String key = cmd.getKey();
        String valueToAppend = cmd.getValue();
        String currentValue = store.getOrDefault(key, "");
        String newValue = currentValue + valueToAppend;
        store.put(key, newValue);
        return new AppendResult(newValue);
    }

}