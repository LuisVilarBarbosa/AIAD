import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageContent {
    private final int initialFloor;
    private final int destinationFloor;
    private final long timeToInitialFloor;
    private static final String separator = "_";
    private static final Pattern pattern = Pattern.compile("(\\d+)" + separator + "(\\d+)" + separator + "(\\d+)");

    public MessageContent(final String message) {
        Matcher matcher = pattern.matcher(message);
        if (matcher.matches()) {
            this.initialFloor = Integer.parseInt(matcher.group(1));
            this.destinationFloor = Integer.parseInt(matcher.group(2));
            this.timeToInitialFloor = Long.parseLong(matcher.group(3));
        } else
            throw new IllegalArgumentException("Invalid format for message: " + message);
    }

    public MessageContent(final int initialFloor, final int destinationFloor, final long timeToInitialFloor) {
        this.initialFloor = initialFloor;
        this.destinationFloor = destinationFloor;
        this.timeToInitialFloor = timeToInitialFloor;
    }

    public int getInitialFloor() {
        return initialFloor;
    }

    public int getDestinationFloor() {
        return destinationFloor;
    }

    public long getTimeToInitialFloor() {
        return timeToInitialFloor;
    }

    @Override
    public String toString() {
        return initialFloor + separator + destinationFloor + separator + timeToInitialFloor;
    }
}
