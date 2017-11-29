public class Request implements Comparable {
    private int source;
    private int destination;
    private boolean attended;

    public Request(int source) {
        this.source = source;
        this.destination = this.source; // assumption
        this.attended = false;
    }

    public Request(String vars) {
        String[] splittedVars = vars.split(" ");
        if (splittedVars.length != 1 && splittedVars.length != 2)
            throw new IllegalArgumentException();
        this.source = Integer.parseInt(splittedVars[0]);
        if (splittedVars.length > 1)
            this.destination = Integer.parseInt(splittedVars[1]);
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
