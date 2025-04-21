package networking;

import java.io.Serializable;

public class Heartbeat extends MessageBody implements Serializable {


    private final int[] latestUndecidedSlots;

    private final int slotOut;

    private final int firstKey;

    private final int round;

    private final boolean isLeader;

    public Heartbeat() {
        this.latestUndecidedSlots = null;
        this.slotOut = -1;
        this.firstKey = -1;
        this.round = -1;
        this.isLeader = false;
    }

    public Heartbeat(int[] latestUndecidedSlots, int slotOut, int firstKey, int round, boolean isLeader) {
        this.latestUndecidedSlots = latestUndecidedSlots;
        this.slotOut = slotOut;
        this.firstKey = firstKey;
        this.round = round;
        this.isLeader = isLeader;
    }

    public int[] getLatestUndecidedSlots() {
        return latestUndecidedSlots;
    }

    public int getSlotOut() {
        return slotOut;
    }

    public int getFirstKey() {
        return firstKey;
    }

    public int getRound() {
        return round;
    }

    public boolean isLeader() {
        return isLeader;
    }
}
