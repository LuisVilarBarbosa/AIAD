import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageContent {
    private final int initialFloor;
    private final int destinationFloor;
    private final long timeToCompleteRequest;
    private static final String separator = "_";
    private static final Pattern pattern = Pattern.compile("(\\d+)" + separator + "(\\d+)" + separator + "(\\d+)");

    public MessageContent(final String message) {
        Matcher matcher = pattern.matcher(message);
        if (matcher.matches()) {
            this.initialFloor = Integer.parseInt(matcher.group(1));
            this.destinationFloor = Integer.parseInt(matcher.group(2));
            this.timeToCompleteRequest = Long.parseLong(matcher.group(3));
        } else
            throw new IllegalArgumentException("Invalid format for message: " + message);
    }

    public MessageContent(final int initialFloor, final int destinationFloor, final long timeToCompleteRequest) {
        this.initialFloor = initialFloor;
        this.destinationFloor = destinationFloor;
        this.timeToCompleteRequest = timeToCompleteRequest;
    }

    public int getInitialFloor() {
        return initialFloor;
    }

    public int getDestinationFloor() {
        return destinationFloor;
    }

    public long getTimeToCompleteRequest() {
        return timeToCompleteRequest;
    }

    @Override
    public String toString() {
        return initialFloor + separator + destinationFloor + separator + timeToCompleteRequest;
    }
}
