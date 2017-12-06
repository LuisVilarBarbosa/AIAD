public class ElevatorProperties {
    private final int maxWeight;
    private final int numFloors;
    private final long movementTime;
    private final long personEntranceTime;
    private final long personExitTime;
    private final boolean keyboardOnRequest;

    public ElevatorProperties(final int maxWeight, final int numFloors, final long movementTime, final long personEntranceTime, final long personExitTime, final boolean keyboardOnRequest) {
        if (maxWeight < 0)
            throw new IllegalArgumentException("Invalid maximum weight: " + maxWeight);
        if (numFloors < 0)
            throw new IllegalArgumentException("Invalid number of floors: " + numFloors);
        if (movementTime < 0)
            throw new IllegalArgumentException("Invalid movement time: " + movementTime);
        this.maxWeight = maxWeight;
        this.numFloors = numFloors;
        this.movementTime = movementTime;
        this.personEntranceTime = personEntranceTime;
        this.personExitTime = personExitTime;
        this.keyboardOnRequest = keyboardOnRequest;
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

    public long getPersonEntranceTime() {
        return personEntranceTime;
    }

    public long getPersonExitTime() {
        return personExitTime;
    }

    public boolean hasKeyboardOnRequest() {
        return keyboardOnRequest;
    }

    public static String getHasKeyboardOnRequestString(boolean keyboardOnRequest) {
        return keyboardOnRequest ? "True" : "False";
    }
}
