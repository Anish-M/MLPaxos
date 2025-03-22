package results;

import results.Result;

public class AMOResult implements Result {
    private final Result result;
    private final long clientId;
    private final long requestId;

    public AMOResult(Result result, long clientId, long requestId) {
        this.result = result;
        this.clientId = clientId;
        this.requestId = requestId;
    }

    public Result getResult() {
        return result;
    }

    public long getClientId() {
        return clientId;
    }

    public long getRequestId() {
        return requestId;
    }

    public String toString() {
        return "AMO Result: " + result.toString() + " Client ID: " + clientId + " Request ID: " + requestId;
    }
}
