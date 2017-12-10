import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import javax.management.timer.Timer;

public class MyAgent extends Agent {
    protected final long timeout = 2 * Timer.ONE_SECOND;

    @Override
    protected void takeDown() {
        super.takeDown();
        deregisterOnDFService();
    }

    @Override
    public void doDelete() {
        super.doDelete();
        display("State change request to 'deleted' performed.");
    }

    protected void blockBehaviour(final long millis, Behaviour behaviour) {
        if (millis > 0)
            behaviour.block(millis);
    }

    protected void registerOnDFService(final String agentType) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(this.getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setName(this.getName());
        sd.setType(agentType);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
            System.exit(MyBoot.exitCodeOnError);
        }
    }

    protected void deregisterOnDFService() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    protected void display(final String str) {
        Console.display(this.getAID().getLocalName() + ": " + str);
    }

    protected DFAgentDescription[] searchOnDFService(String agentType) {
        DFAgentDescription dfAgentDescription = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(agentType);
        dfAgentDescription.addServices(serviceDescription);
        SearchConstraints searchConstraints = new SearchConstraints();
        searchConstraints.setMaxResults((long) -1);
        DFAgentDescription dfAgentDescriptions[] = new DFAgentDescription[0];
        try {
            dfAgentDescriptions = DFService.search(this, dfAgentDescription, searchConstraints);
        } catch (FIPAException e) {
            e.printStackTrace();
            System.exit(MyBoot.exitCodeOnError);
        }
        return dfAgentDescriptions;
    }
}
