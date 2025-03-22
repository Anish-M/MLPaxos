package results;

public class GetResult extends KVStoreResult {
    private String value;

    public GetResult(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String toString() {
        return "GetResult: " + value;
    }
}