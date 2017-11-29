public class ElevatorMessage {
    private int source;
    private Integer destination;
    private int distanceToSource;
    private static final String separator = " ";

    public static ElevatorMessage parseMessage(String message) {
        String[] parts = message.split(separator);
        if(parts.length == 2){
            int source = Integer.parseInt(parts[0]);
            Integer destination = null;
            int distanceToSource = Integer.parseInt(parts[1]);
            return new ElevatorMessage(source, destination, distanceToSource);
        }
        else if(parts.length == 3){
            int source = Integer.parseInt(parts[0]);
            int destination = Integer.parseInt(parts[1]);
            int distanceToSource = Integer.parseInt(parts[2]);
            return new ElevatorMessage(source, destination, distanceToSource);
        }
        else
            throw new IllegalArgumentException("Invalid format for message");
    }

    private ElevatorMessage(int source, int destination, int distanceToSource) {
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
        return source + separator + (destination != null ? destination + separator : "") + distanceToSource;
    }
}