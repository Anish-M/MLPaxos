package results;

public class AppendResult extends KVStoreResult {
    private String newValue;

    public AppendResult(String newValue) {
        this.newValue = newValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String toString() {
        return "AppendResult: " + newValue;
    }
}