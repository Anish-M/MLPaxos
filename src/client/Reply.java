package client;

import results.Result;
import java.io.Serializable;

public class Reply implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Result result;

    private final long serverId;

    private final long replyId;

    public Reply(Result result, long serverId, long replyId) {
        this.result = result;
        this.serverId = serverId;
        this.replyId = replyId;
    }

    public Result getResult() {
        return result;
    }

    public long getServerId() {
        return serverId;
    }

    public String socketReply() {
        return "Reply: " + result.toString() + " Server ID: " + serverId + " Reply ID: " + replyId;
    }
}