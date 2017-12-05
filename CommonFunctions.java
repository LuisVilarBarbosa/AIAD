import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class CommonFunctions {

    public static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            MyBoot.logger.warning(e.toString());
        }
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
            MyBoot.logger.warning(e.toString());
        }
    }
}
