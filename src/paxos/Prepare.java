package paxos;

public class Prepare {
    private int proposalId;
    private Object value; // This may or may not be set.

    public Prepare(int proposalId) {
        this.proposalId = proposalId;
        this.value = null;  // Value is not necessarily provided in Phase 1a
    }

    public int getProposalId() {
        return proposalId;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Prepare{proposalId=" + proposalId + ", value=" + value + '}';
    }
}
