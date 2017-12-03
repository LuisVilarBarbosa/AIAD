public class ElevatorProperties {
    private final int maxWeight;
    private final int numFloors;
    private final long movementTime;

    public ElevatorProperties(int maxWeight, int numFloors, long movementTime) {
        if (maxWeight < 0)
            throw new IllegalArgumentException("Invalid maximum weight: " + maxWeight);
        if (numFloors < 0)
            throw new IllegalArgumentException("Invalid number of floors: " + numFloors);
        if (movementTime < 0)
            throw new IllegalArgumentException("Invalid movement time: " + movementTime);
        this.maxWeight = maxWeight;
        this.numFloors = numFloors;
        this.movementTime = movementTime;
    }

    public int getMaxWeight() {
        return maxWeight;
    }

    public int getNumFloors() {
        return numFloors;
    }

    public long getMovementTime() {
        return movementTime;
    }
}
