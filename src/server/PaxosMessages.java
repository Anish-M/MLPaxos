package server;
import java.util.*;
import networking.MessageBody;
import commands.Command;


class Prepare extends MessageBody {
    private final int round;

    public Prepare(int round) {
        this.round = round;
    }

    public int getRound() {
        return round;
    }
}

class Prepared extends MessageBody {
    private final Log log;
    private final int round;

    public Prepared(Log log, int round) {
        this.log = log;
        this.round = round;
    }

    public Log getLog() {
        return log;
    }

    public int getRound() {
        return round;
    }
}

class Propose extends MessageBody {
    private final LogEntry vote;

    public Propose(LogEntry vote) {
        this.vote = vote;
    }

    public LogEntry getVote() {
        return vote;
    }
}

class ProposeBatch extends MessageBody {
    private final List<Propose> votes;

    public ProposeBatch(List<Propose> votes) {
        this.votes = votes;
    }

    public List<Propose> getVotes() {
        return votes;
    }
}


class Accept extends MessageBody {
    private final LogEntry vote;

    public Accept(LogEntry vote) {
        this.vote = vote;
    }

    public LogEntry getVote() {
        return vote;
    }
}

class Decide extends MessageBody {
    private final LogEntry vote;

    public Decide(LogEntry vote) {
        this.vote = vote;
    }

    public LogEntry getVote() {
        return vote;
    }
}

class CatchupDecide extends MessageBody {
    private final LogEntry vote;

    public CatchupDecide(LogEntry vote) {
        this.vote = vote;
    }

    public LogEntry getVote() {
        return vote;
    }
}

class BatchCatchupDecide extends MessageBody {
    private final List<CatchupDecide> votes;

    public BatchCatchupDecide(List<CatchupDecide> votes) {
        this.votes = votes;
    }

    public List<CatchupDecide> getVotes() {
        return votes;
    }


}

class FirstUndecidedSlot extends MessageBody {
    private final int slot;

    public FirstUndecidedSlot(int slot) {
        this.slot = slot;
    }

    public int getSlot() {
        return slot;
    }

}



class PValue {
    /* Let a pvalue be a triple consisting of a
    ballot number, a slot number, and a command. */
    private final int round;
    private final int slot;
    private final Command command;

    public PValue() {
        this.round = -1;
        this.slot = -1;
        this.command = null;
    }
    public PValue(int round, int slot, Command command) {
        this.round = round;
        this.slot = slot;
        this.command = command;
    }

    public int getRound() {
        return round;
    }

    public int getSlot() {
        return slot;
    }

    public Command getCommand() {
        return command;
    }
}


