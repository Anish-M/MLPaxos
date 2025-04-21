package server;
import java.io.Serializable;
import commands.AMOCommand;

class LogEntry implements Serializable {
    private final int slot;
    private final int round;
    private final AMOCommand command;

    private final PaxosLogSlotStatus status;

    public LogEntry(int slot, int round, AMOCommand command, PaxosLogSlotStatus status) {
        this.slot = slot;
        this.round = round;
        this.command = command;
        this.status = status;
    }

    public int getSlot() {
        return slot;
    }

    public AMOCommand getCommand() {
        return command;
    }

    public PaxosLogSlotStatus getStatus() {
        return status;
    }

    public int getRound() {
        return round;
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "slot=" + slot +
                ", round=" + round +
                ", command=" + command +
                ", status=" + status +
                '}';
    }
}