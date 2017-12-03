public class ElevatorState {
    public static final int STOPPED = 0;
    public static final int GOING_UP = 1;
    public static final int GOING_DOWN = 2;
    private int currentFloor;
    private int currentWeight;
    private int movementState;
    private int numPeople;

    public ElevatorState() {
        this.currentFloor = 0;
        this.currentWeight = 0;
        this.movementState = STOPPED;
        this.numPeople = 0;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public void setCurrentFloor(int currentFloor) {
        if (currentFloor < 0)
            throw new IllegalArgumentException("Invalid current floor: " + currentFloor);
        this.currentFloor = currentFloor;
    }

    public int getCurrentWeight() {
        return currentWeight;
    }

    public void setCurrentWeight(int currentWeight) {
        if (currentWeight < 0)
            throw new IllegalArgumentException("Invalid current weight: " + currentWeight);
        this.currentWeight = currentWeight;
    }

    public int getMovementState() {
        return movementState;
    }

    public void setMovementState(int movementState) {
        if (movementState != STOPPED && movementState != GOING_UP && movementState != GOING_DOWN)
            throw new IllegalArgumentException("Invalid movement state: " + movementState);
        else
            this.movementState = movementState;
    }

    public int getNumPeople() {
        return numPeople;
    }

    public void setNumPeople(int numPeople) {
        if (numPeople < 0)
            throw new IllegalArgumentException("Invalid number of people: " + numPeople);
        this.numPeople = numPeople;
    }

    public static String getMovementStateString(int movementState) {
        switch (movementState) {
            case STOPPED:
                return "Stopped";
            case GOING_UP:
                return "Going up";
            case GOING_DOWN:
                return "Going down";
            default:
                throw new IllegalArgumentException("Invalid movement state: " + movementState);
        }
    }
}
