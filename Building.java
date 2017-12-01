import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import javax.management.timer.Timer;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;

public class Building extends Agent {
    public static final String agentType = "Building";
    private final int numFloors;
    private final int numElevators;
    private final ArrayList<Integer> maxWeights;
    private final Random random;
    private final ArrayList<AID> elevators;
    private final TreeSet<Request> externalRequests;
    private final ArrayList<DFAgentDescription> descriptions;

    public Building(int numFloors, int numElevators, ArrayList<Integer> maxWeights) {
        if (maxWeights.size() != numElevators)
            throw new IllegalArgumentException();
        this.numFloors = numFloors;
        this.numElevators = numElevators;
        this.maxWeights = maxWeights;
        this.random = new Random();
        this.elevators = new ArrayList<>();
        this.externalRequests = new TreeSet<>();
        this.descriptions = new ArrayList<>();
    }

    protected void setup() {
        super.setup();
        DFAgentDescription dfd = new DFAgentDescription();
        Behaviour b = new SubscriptionInitiator(
                this, DFService.createSubscriptionMessage(this, getDefaultDF(), dfd, null)) {
            protected void handleInform(ACLMessage inform) {
                try {
                    DFAgentDescription[] dfds = DFService.decodeNotification(inform.getContent());
                    for (DFAgentDescription d : dfds) {
                        descriptions.add(d);
                        //System.out.println("Agent " + d.getName().getName() + " created by building.");
                    }
                } catch (FIPAException e) {
                    e.printStackTrace();
                }

            }
        };
        addBehaviour(b);
        generateElevatorsAgents();
        addBehaviour(new BuildingBehaviour());
    }

    private void generateElevatorsAgents() {
        for (int i = 0; i < numElevators; i++)
            try {
                Elevator elevator = new Elevator(maxWeights.get(i), numFloors);
                AgentController ac = this.getContainerController().acceptNewAgent(Elevator.agentType + i, elevator);
                ac.start();
                elevators.add(elevator.getAID());
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
    }

    private class BuildingBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            int n = random.nextInt() % (3 * numElevators);
            generateNRequests(n);
            sendRequests();
            externalRequests.clear();
            sleep(10 * Timer.ONE_SECOND);
        }

        private void addRequest(int floor) {
            if (floor < 0 || floor > numFloors)
                throw new IllegalArgumentException("Invalid floor.");
            Request request = new Request(floor);
            if (!externalRequests.contains(request))
                externalRequests.add(request);
        }

        private void generateNRequests(int n) {
            for (int i = 0; i < n; i++) {
                int rand = Math.abs(random.nextInt() % (numFloors * 2));
                if (rand > numFloors)
                    addRequest(0);
                else
                    addRequest(rand);
            }
        }

        private void sendRequests() {
            for (Request request : externalRequests) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setSender(this.getAgent().getAID());
                msg.addReceiver(elevators.get(random.nextInt(elevators.size())));
                msg.setProtocol(agentType);
                msg.setContent(Integer.toString(request.getSource()));
                send(msg);
            }
        }

        private void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
