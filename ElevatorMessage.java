import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElevatorMessage {
    private final int source;
    private final int destination;
    private final int distanceToSource;
    private static final String separator = "_";
    private static final Pattern pattern = Pattern.compile("(\\d+)" + separator + "(\\d+)" + separator + "(\\d+)");

    public ElevatorMessage(final String message) {
        Matcher matcher = pattern.matcher(message);
        if (matcher.matches()) {
            this.source = Integer.parseInt(matcher.group(1));
            this.destination = Integer.parseInt(matcher.group(2));
            this.distanceToSource = Integer.parseInt(matcher.group(3));
        } else
            throw new IllegalArgumentException("Invalid format for message");
    }

    public ElevatorMessage(final int source, final int destination, final int distanceToSource) {
        this.source = source;
        this.destination = destination;
        this.distanceToSource = distanceToSource;
    }

    public int getSource() {
        return source;
    }

    public int getDestination() {
        return destination;
    }

    public int getDistanceToSource() {
        return distanceToSource;
    }

    @Override
    public String toString() {
        return source + separator + destination + separator + distanceToSource;
    }
}
