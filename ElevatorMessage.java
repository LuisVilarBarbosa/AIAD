import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElevatorMessage {
    private final int initialFloor;
    private final int destinationFloor;
    private final int distanceToInitialFloor;
    private static final String separator = "_";
    private static final Pattern pattern = Pattern.compile("(\\d+)" + separator + "(\\d+)" + separator + "(\\d+)");

    public ElevatorMessage(final String message) {
        Matcher matcher = pattern.matcher(message);
        if (matcher.matches()) {
            this.initialFloor = Integer.parseInt(matcher.group(1));
            this.destinationFloor = Integer.parseInt(matcher.group(2));
            this.distanceToInitialFloor = Integer.parseInt(matcher.group(3));
        } else
            throw new IllegalArgumentException("Invalid format for message");
    }

    public ElevatorMessage(final int initialFloor, final int destinationFloor, final int distanceToInitialFloor) {
        this.initialFloor = initialFloor;
        this.destinationFloor = destinationFloor;
        this.distanceToInitialFloor = distanceToInitialFloor;
    }

    public int getInitialFloor() {
        return initialFloor;
    }

    public int getDestinationFloor() {
        return destinationFloor;
    }

    public int getDistanceToInitialFloor() {
        return distanceToInitialFloor;
    }

    @Override
    public String toString() {
        return initialFloor + separator + destinationFloor + separator + distanceToInitialFloor;
    }
}
