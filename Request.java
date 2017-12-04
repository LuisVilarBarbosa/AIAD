public class Request implements Comparable {
    private final int initialFloor;
    private int destinationFloor;
    private boolean attended;
    private final long creationTime;

    public Request(final int initialFloor) {
        this.initialFloor = initialFloor;
        this.destinationFloor = this.initialFloor; // assumption
        this.attended = false;
        this.creationTime = System.currentTimeMillis();
    }

    public Request(final String vars) {
        final String[] splitVars = vars.split(" ");
        if (splitVars.length != 1 && splitVars.length != 2)
            throw new IllegalArgumentException();
        this.initialFloor = Integer.parseInt(splitVars[0]);
        if (splitVars.length > 1)
            this.destinationFloor = Integer.parseInt(splitVars[1]);
        else
            this.destinationFloor = this.initialFloor; // assumption
        this.attended = false;
        this.creationTime = System.currentTimeMillis();
    }

    public void setDestinationFloor(int destinationFloor) {
        this.destinationFloor = destinationFloor;
    }

    public void setAttended() {
        this.attended = true;
    }

    public int getInitialFloor() {
        return initialFloor;
    }

    public int getDestinationFloor() {
        return destinationFloor;
    }

    public boolean isAttended() {
        return attended;
    }

    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public int compareTo(Object o) {
        Request r = (Request) o;
        if (this.attended && r.attended)
            return Integer.compare(this.destinationFloor, r.destinationFloor);
        else if (this.attended/* && !r.attended*/)
            return Integer.compare(this.destinationFloor, r.initialFloor);
        else if (/*!this.attended && */r.attended)
            return Integer.compare(this.initialFloor, r.destinationFloor);
        else
            return Integer.compare(this.initialFloor, r.initialFloor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Request request = (Request) o;

        if (initialFloor != request.initialFloor) return false;
        if (destinationFloor != request.destinationFloor) return false;
        return attended == request.attended;
    }
}
