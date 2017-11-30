import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElevatorState {
    private int actualFloor;
    private int actualWeight;
    private int internalRequestsSize;
    private ArrayList<String> information;
    private static final String separator = "§";
    private static final Pattern pattern = Pattern.compile("(\\d+)" + separator + "(\\d+)" + separator + "(\\d+)(" + separator + "(.+))?");

    public ElevatorState(String message) {
        Matcher matcher = pattern.matcher(message);
        if (matcher.matches()) {
            this.actualFloor = Integer.parseInt(matcher.group(1));
            this.actualWeight = Integer.parseInt(matcher.group(2));
            this.internalRequestsSize = Integer.parseInt(matcher.group(3));
            this.information = new ArrayList<>();
            if (matcher.groupCount() >= 5) {
                String[] infoParts = matcher.group(5).split(separator);
                for (String str : infoParts)
                    this.information.add(str);
            }
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
