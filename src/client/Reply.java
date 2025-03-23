package client;

import networking.MessageBody;
import results.Result;
import java.io.Serializable;

public class Reply extends MessageBody implements Serializable {
    private final Result result;

    public Reply(Result result) {
        this.result = result;
    }

    public Result getResult() {
        return result;
    }
}
