import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElevatorState {
    private final int actualFloor;
    private final int actualWeight;
    private final int internalRequestsSize;
    private final String state;
    private final ArrayList<String> information;
    private final int nextFloorToStop;
    private final int numPeople;
    private final int maxWeight;
    private final long movementTime;
    private static final String separator = "§";
    private static final Pattern pattern = Pattern.compile("(\\d+)" + separator + "(\\d+)" + separator + "(\\d+)" + separator + "(\\d+)" + separator + "(\\d+)" + separator + "(\\d+)" + separator + "(\\d+)(" + separator + "(.+))");

    public ElevatorState(final String message) {
        final Matcher matcher = pattern.matcher(message);
        if (matcher.matches()) {
            this.actualFloor = Integer.parseInt(matcher.group(1));
            this.actualWeight = Integer.parseInt(matcher.group(2));
            this.internalRequestsSize = Integer.parseInt(matcher.group(3));
            this.nextFloorToStop = Integer.parseInt(matcher.group(4));
            this.numPeople = Integer.parseInt(matcher.group(5));
            this.maxWeight = Integer.parseInt(matcher.group(6));
            this.movementTime = Long.parseLong(matcher.group(7));
            final String[] stateAndInfoParts = matcher.group(9).split(separator);
            this.state = stateAndInfoParts[0];
            this.information = new ArrayList<>();
            for (int i = 1; i < stateAndInfoParts.length; i++)
                this.information.add(stateAndInfoParts[i]);
        } else
            throw new IllegalArgumentException("Invalid format for message: " + message);
    }

    public ElevatorState(int actualFloor, int actualWeight, int internalRequestsSize, String state, ArrayList<String> information, int nextFloorToStop, int numPeople, int maxWeight, long movementTime) {
        this.actualFloor = actualFloor;
        this.actualWeight = actualWeight;
        this.internalRequestsSize = internalRequestsSize;
        this.state = state;
        this.information = information;
        this.nextFloorToStop = nextFloorToStop;
        this.numPeople = numPeople;
        this.maxWeight = maxWeight;
        this.movementTime = movementTime;
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

    public String getState() {
        return state;
    }

    public ArrayList<String> getInformation() {
        return information;
    }

    public int getNextFloorToStop() {
        return nextFloorToStop;
    }

    public int getNumPeople() {
        return numPeople;
    }

    public int getMaxWeight() {
        return maxWeight;
    }

    public long getMovementTime() {
        return movementTime;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(actualFloor);
        sb.append(separator).append(actualWeight);
        sb.append(separator).append(internalRequestsSize);
        sb.append(separator).append(nextFloorToStop);
        sb.append(separator).append(numPeople);
        sb.append(separator).append(maxWeight);
        sb.append(separator).append(movementTime);
        sb.append(separator).append(state);
        for (final String info : information)
            sb.append(separator).append(info);
        return sb.toString();
    }
}
