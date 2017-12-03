public class ElevatorStatistics {
    private long peopleEntranceTime;
    private long peopleExitTime;
    private long maxWaitTime;
    private long minWaitTime;

    public long getPeopleEntranceTime() {
        return peopleEntranceTime;
    }

    public void setPeopleEntranceTime(long peopleEntranceTime) {
        if (peopleEntranceTime < 0)
            throw new IllegalArgumentException("Invalid people entrance time: " + peopleEntranceTime);
        this.peopleEntranceTime = peopleEntranceTime;
    }

    public long getPeopleExitTime() {
        return peopleExitTime;
    }

    public void setPeopleExitTime(long peopleExitTime) {
        if (peopleExitTime < 0)
            throw new IllegalArgumentException("Invalid people exit time: " + peopleExitTime);
        this.peopleExitTime = peopleExitTime;
    }

    public long getMaxWaitTime() {
        return maxWaitTime;
    }

    public void setMaxWaitTime(long maxWaitTime) {
        if (maxWaitTime < 0)
            throw new IllegalArgumentException("Invalid maximum wait time: " + maxWaitTime);
        this.maxWaitTime = maxWaitTime;
    }

    public long getMinWaitTime() {
        return minWaitTime;
    }

    public void setMinWaitTime(long minWaitTime) {
        if (minWaitTime < 0)
            throw new IllegalArgumentException("Invalid minimum wait time: " + minWaitTime);
        this.minWaitTime = minWaitTime;
    }
}
