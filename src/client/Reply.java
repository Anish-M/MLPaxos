package client;

import results.Result;
import java.io.Serializable;

public class Reply {
    private final Result result;

    public Reply(Result result) {
        this.result = result;
    }

    public Result getResult() {
        return result;
    }
}
