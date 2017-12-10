public class ElevatorProperties {
    private final int maxWeight;
    private final long movementTime;
    private final long personEntranceTime;
    private final long personExitTime;

    public ElevatorProperties(final int maxWeight, final long movementTime, final long personEntranceTime, final long personExitTime) {
        if (maxWeight < 0)
            throw new IllegalArgumentException("Invalid maximum weight: " + maxWeight);
        if (movementTime < 0)
            throw new IllegalArgumentException("Invalid movement time: " + movementTime);
        this.maxWeight = maxWeight;
        this.movementTime = movementTime;
        this.personEntranceTime = personEntranceTime;
        this.personExitTime = personExitTime;
    }

    public int getMaxWeight() {
        return maxWeight;
    }

    public long getMovementTime() {
        return movementTime;
    }

    public long getPersonEntranceTime() {
        return personEntranceTime;
    }

    public long getPersonExitTime() {
        return personExitTime;
    }
}
