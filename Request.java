public class Request implements Comparable {
    private final int initialFloor;
    private int destinationFloor;
    private boolean attended;
    private final long creationTime;
    private int weight;

    public Request(final int initialFloor) {
        this(initialFloor, initialFloor);
    }

    public Request(final int initialFloor, final int destinationFloor) {
        this.initialFloor = initialFloor;
        this.destinationFloor = destinationFloor;
        this.attended = false;
        this.creationTime = System.currentTimeMillis();
        this.weight = 0;
    }

    public void setDestinationFloor(int destinationFloor) {
        this.destinationFloor = destinationFloor;
    }

    public void setAttended(final int weight) {
        this.attended = true;
        this.weight = weight;
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

    public int getWeight() {
        return weight;
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
        return initialFloor == request.initialFloor &&
                destinationFloor == request.destinationFloor &&
                attended == request.attended &&
                creationTime == request.creationTime &&
                weight == request.weight;
    }
}
