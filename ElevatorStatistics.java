public class ElevatorStatistics {
    private int CFPsSent;
    private int proposesSent;
    private int refusesSent;
    private int acceptedProposalsSent;
    private int acceptedProposalsReceived;
    private long peopleEntranceTime;
    private long peopleExitTime;
    private long maxWaitTime;
    private long minWaitTime;
    private long uptime;
    private long downtime;
    private double useRate;

    public ElevatorStatistics() {
        this.CFPsSent = 0;
        this.proposesSent = 0;
        this.refusesSent = 0;
        this.acceptedProposalsSent = 0;
        this.acceptedProposalsReceived = 0;
        this.peopleEntranceTime = 0;
        this.peopleExitTime = 0;
        this.maxWaitTime = 0;
        this.minWaitTime = 0;
        this.uptime = 0;
        this.downtime = 0;
        this.useRate = 0;
    }

    public int getCFPsSent() {
        return CFPsSent;
    }

    public void setCFPsSent(int CFPsSent) {
        if (CFPsSent < 0)
            throw new IllegalArgumentException("Invalid number of CFPs sent: " + CFPsSent);
        this.CFPsSent = CFPsSent;
    }

    public int getProposesSent() {
        return proposesSent;
    }

    public void setProposesSent(int proposesSent) {
        if (proposesSent < 0)
            throw new IllegalArgumentException("Invalid number of proposes sent: " + proposesSent);
        this.proposesSent = proposesSent;
    }

    public int getRefusesSent() {
        return refusesSent;
    }

    public void setRefusesSent(int refusesSent) {
        if (refusesSent < 0)
            throw new IllegalArgumentException("Invalid number of refuses sent: " + refusesSent);
        this.refusesSent = refusesSent;
    }

    public int getAcceptedProposalsSent() {
        return acceptedProposalsSent;
    }

    public void setAcceptedProposalsSent(int acceptedProposalsSent) {
        if (acceptedProposalsSent < 0)
            throw new IllegalArgumentException("Invalid number of accepted proposals sent: " + acceptedProposalsSent);
        this.acceptedProposalsSent = acceptedProposalsSent;
    }

    public int getAcceptedProposalsReceived() {
        return acceptedProposalsReceived;
    }

    public void setAcceptedProposalsReceived(int acceptedProposalsReceived) {
        if (acceptedProposalsReceived < 0)
            throw new IllegalArgumentException("Invalid number of accepted proposals received: " + acceptedProposalsReceived);
        this.acceptedProposalsReceived = acceptedProposalsReceived;
    }

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

    public long getUptime() {
        return uptime;
    }

    public void setUptime(long uptime) {
        if (uptime < 0)
            throw new IllegalArgumentException("Invalid uptime: " + uptime);
        this.uptime = uptime;
        updateUseRate();
    }

    public long getDowntime() {
        return downtime;
    }

    public void setDowntime(long downtime) {
        if (downtime < 0)
            throw new IllegalArgumentException("Invalid downtime: " + downtime);
        this.downtime = downtime;
        updateUseRate();
    }

    public double getUseRate() {
        return useRate;
    }

    private void updateUseRate() {
        this.useRate = this.uptime * 100.0 / (this.uptime + this.downtime);
    }
}
