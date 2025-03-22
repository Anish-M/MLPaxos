package results;

import results.Result;

public class AMOResult implements Result {
    private final Result result;

    public AMOResult(Result result) {
        this.result = result;
    }

    public Result getResult() {
        return result;
    }

    public String toString() {
        return "AMO Result: " + result.toString();
    }
}
