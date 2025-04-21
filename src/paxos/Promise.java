package paxos;

public class Promise {
    private int proposalId;
    private int highestAcceptedId;
    private Object acceptedValue;

    public Promise(int proposalId, int highestAcceptedId, Object acceptedValue) {
        this.proposalId = proposalId;
        this.highestAcceptedId = highestAcceptedId;
        this.acceptedValue = acceptedValue;
    }

    public int getProposalId() {
        return proposalId;
    }

    public int getHighestAcceptedId() {
        return highestAcceptedId;
    }

    public Object getAcceptedValue() {
        return acceptedValue;
    }

    @Override
    public String toString() {
        return "Promise{proposalId=" + proposalId + ", highestAcceptedId=" + highestAcceptedId + ", acceptedValue=" + acceptedValue + '}';
    }
}
