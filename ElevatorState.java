import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElevatorState {
    private final int actualFloor;
    private final int actualWeight;
    private final int internalRequestsSize;
    private final String state;
    private final ArrayList<String> information;
    private static final String separator = "§";
    private static final Pattern pattern = Pattern.compile("(\\d+)" + separator + "(\\d+)" + separator + "(\\d+)(" + separator + "(.+))");

    public ElevatorState(final String message) {
        final Matcher matcher = pattern.matcher(message);
        if (matcher.matches()) {
            this.actualFloor = Integer.parseInt(matcher.group(1));
            this.actualWeight = Integer.parseInt(matcher.group(2));
            this.internalRequestsSize = Integer.parseInt(matcher.group(3));
            final String[] stateAndInfoParts = matcher.group(5).split(separator);
            this.state = stateAndInfoParts[0];
            this.information = new ArrayList<>();
            for (int i = 1; i < stateAndInfoParts.length; i++)
                this.information.add(stateAndInfoParts[i]);
        } else
            throw new IllegalArgumentException("Invalid format for message");
    }

    public ElevatorState(final int actualFloor, final int actualWeight, final int internalRequestsSize, final String state, final ArrayList<String> information) {
        this.actualFloor = actualFloor;
        this.actualWeight = actualWeight;
        this.internalRequestsSize = internalRequestsSize;
        this.state = state;
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

    public String getState() {
        return state;
    }

    public ArrayList<String> getInformation() {
        return information;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(actualFloor).append(separator).append(actualWeight).append(separator).append(internalRequestsSize).append(separator).append(state);
        for (final String info : information)
            sb.append(separator).append(info);
        return sb.toString();
    }
}
