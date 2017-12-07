import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class CommonFunctions {

    public static void sleep(final long millis, Behaviour behaviour) {
        long endMillis = System.currentTimeMillis() + millis;
        behaviour.block(millis);    // does not work as expected
        while (System.currentTimeMillis() <= endMillis)
            behaviour.block(endMillis - System.currentTimeMillis());
    }

    public static void registerOnDFService(final Agent agent, final String agentType) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(agent.getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setName(agent.getName());
        sd.setType(agentType);
        dfd.addServices(sd);
        try {
            DFService.register(agent, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    public static void deregisterOnDFService(final Agent agent) {
        try {
            DFService.deregister(agent);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}
