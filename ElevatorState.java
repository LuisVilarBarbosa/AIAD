public class ElevatorState {
    private int actualFloor;
    private int actualWeight;
    private int internalRequestsSize;
    private static final String separator = " ";

    public ElevatorState(String message) {
        String[] parts = message.split(separator);
        if (parts.length == 3) {
            this.actualFloor = Integer.parseInt(parts[0]);
            this.actualWeight = Integer.parseInt(parts[1]);
            this.internalRequestsSize = Integer.parseInt(parts[2]);
        } else
            throw new IllegalArgumentException("Invalid format for message");
    }

    public ElevatorState(int actualFloor, int actualWeight, int internalRequestsSize) {
        this.actualFloor = actualFloor;
        this.actualWeight = actualWeight;
        this.internalRequestsSize = internalRequestsSize;
    }

    public int getActualFloor() {
        return actualFloor;
    }

    public int getActualWeight() {
        return actualWeight;
    }

    public int getInternalRequestsSize() {
        return internalRequestsSize;
    }

    @Override
    public String toString() {
        return actualFloor + separator + actualWeight + separator + internalRequestsSize;
    }
}
