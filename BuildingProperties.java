public class BuildingProperties {
    private final int numFloors;
    private final int numElevators;
    private final boolean keyboardOnRequest;
    private final boolean elevatorsNegotiationAllowed;

    public BuildingProperties(int numFloors, int numElevators, boolean keyboardOnRequest, boolean elevatorsNegotiationAllowed) {
        if (numFloors < 2)
            throw new IllegalArgumentException("Invalid number of floors: " + numFloors);
        if (numElevators < 0)
            throw new IllegalArgumentException("Invalid number of elevators: " + numElevators);
        this.numFloors = numFloors;
        this.numElevators = numElevators;
        this.keyboardOnRequest = keyboardOnRequest;
        this.elevatorsNegotiationAllowed = elevatorsNegotiationAllowed;
    }

    public int getNumFloors() {
        return numFloors;
    }

    public int getNumElevators() {
        return numElevators;
    }

    public boolean hasKeyboardOnRequest() {
        return keyboardOnRequest;
    }

    public boolean isElevatorsNegotiationAllowed() {
        return elevatorsNegotiationAllowed;
    }
}
