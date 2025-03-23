package results;

import results.Result;

public class AMOResult implements Result {
    private final Result result;

    private final long requestId;

    public AMOResult(Result result, long requestId) {
        this.result = result;
        this.requestId = requestId;
    }

    public Result getResult() {
        return result;
    }

    public long getRequestId() {
        return requestId;
    }

    public String toString() {
        return "AMO Result: " + result.toString();
    }
}
