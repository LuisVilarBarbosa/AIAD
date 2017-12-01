public class Request implements Comparable {
    private final int source;
    private int destination;
    private boolean attended;

    public Request(final int source) {
        this.source = source;
        this.destination = this.source; // assumption
        this.attended = false;
    }

    public Request(final String vars) {
        final String[] splitVars = vars.split(" ");
        if (splitVars.length != 1 && splitVars.length != 2)
            throw new IllegalArgumentException();
        this.source = Integer.parseInt(splitVars[0]);
        if (splitVars.length > 1)
            this.destination = Integer.parseInt(splitVars[1]);
        else
            this.destination = this.source; // assumption
        this.attended = false;

    }

    public void setDestination(int destination) {
        this.destination = destination;
    }

    public void setAttended() {
        this.attended = true;
    }

    public int getSource() {
        return source;
    }

    public int getDestination() {
        return destination;
    }

    public boolean isAttended() {
        return attended;
    }

    @Override
    public int compareTo(Object o) {
        Request r = (Request) o;
        if (this.attended && r.attended)
            return Integer.compare(this.destination, r.destination);
        else if (this.attended && !r.attended)
            return Integer.compare(this.destination, r.source);
        else if (!this.attended && r.attended)
            return Integer.compare(this.source, r.destination);
        else
            return Integer.compare(this.source, r.source);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Request request = (Request) o;

        if (source != request.source) return false;
        if (destination != request.destination) return false;
        return attended == request.attended;
    }
}
