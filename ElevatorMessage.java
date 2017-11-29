public class ElevatorMessage {
    private int source;
    private int destination;
    private int distanceToSource;
    private static final String separator = " ";

    public ElevatorMessage(String message) {
        String[] parts = message.split(separator);
        if (parts.length == 3) {
            this.source = Integer.parseInt(parts[0]);
            this.destination = Integer.parseInt(parts[1]);
            this.distanceToSource = Integer.parseInt(parts[2]);
        } else
            throw new IllegalArgumentException("Invalid format for message");
    }

    public ElevatorMessage(int source, int destination, int distanceToSource) {
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
