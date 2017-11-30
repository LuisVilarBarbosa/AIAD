import java.util.ArrayList;

public class ElevatorState {
    private int actualFloor;
    private int actualWeight;
    private int internalRequestsSize;
    private ArrayList<String> information;
    private static final String separator = "_";

    public ElevatorState(String message) {
        String[] parts = message.split(separator);
        if (parts.length >= 3) {
            this.actualFloor = Integer.parseInt(parts[0]);
            this.actualWeight = Integer.parseInt(parts[1]);
            this.internalRequestsSize = Integer.parseInt(parts[2]);
            this.information = new ArrayList<>();
            for (int i = 3; i < parts.length; i++)
                this.information.add(parts[i]);
        } else
            throw new IllegalArgumentException("Invalid format for message");
    }

    public ElevatorState(int actualFloor, int actualWeight, int internalRequestsSize, ArrayList<String> information) {
        this.actualFloor = actualFloor;
        this.actualWeight = actualWeight;
        this.internalRequestsSize = internalRequestsSize;
        this.information = information;
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

    public ArrayList<String> getInformation() {
        return information;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(actualFloor).append(separator).append(actualWeight).append(separator).append(internalRequestsSize);
        for (String info : information)
            sb.append(separator).append(info);
        return sb.toString();
    }
}
