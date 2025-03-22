package results;

public class KeyNotFound extends KVStoreResult {
    private String key;

    public KeyNotFound(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String toString() {
        return "KeyNotFound: " + key;
    }
}