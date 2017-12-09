import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class MyAgent extends Agent {

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

    protected void displayError(final String str) {
        Console.displayError(this.getAID().getLocalName() + ": " + str);
    }
}
