import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import javax.management.timer.Timer;
import java.util.ArrayList;
import java.util.Random;

public class Building extends Agent {
    public static final String agentType = "Building";
    private final int numFloors;
    private final int numElevators;
    private final ArrayList<Integer> maxWeights;
    private final Random random;
    private final ArrayList<AID> elevators;
    private final ArrayList<DFAgentDescription> descriptions;

    public Building(final int numFloors, final int numElevators, final ArrayList<Integer> maxWeights) {
        super();
        if (numFloors < 0)
            throw new IllegalArgumentException("Invalid number of floors:" + numFloors);
        if (numElevators < 0)
            throw new IllegalArgumentException("Invalid number of elevators: " + numElevators);
        if (maxWeights.size() != numElevators)
            throw new IllegalArgumentException("Number of maximum weights for elevators different than the number of elevators.");
        this.numFloors = numFloors;
        this.numElevators = numElevators;
        this.maxWeights = maxWeights;
        this.random = new Random();
        this.elevators = new ArrayList<>();
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
        final AgentContainer containerController = this.getContainerController();
        for (int i = 0; i < numElevators; i++)
            try {
                final Elevator elevator = new Elevator(maxWeights.get(i), numFloors);
                final AgentController ac = containerController.acceptNewAgent(Elevator.agentType + i, elevator);
                ac.start();
                elevators.add(elevator.getAID());
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
    }

    private class BuildingBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            final int n = random.nextInt() % (3 * numElevators);
            final ArrayList<Request> requests = generateNRequests(n);
            sendRequests(requests);
            CommonFunctions.sleep(10 * Timer.ONE_SECOND);
        }

        private void addRequest(final int floor, final ArrayList<Request> requests) {
            if (floor < 0 || floor > numFloors)
                throw new IllegalArgumentException("Invalid floor: " + floor);
            final Request request = new Request(floor);
            requests.add(request);
        }

        private ArrayList<Request> generateNRequests(final int n) {
            final ArrayList<Request> requests = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                final int rand = random.nextInt(numFloors * 2);
                final int floor = rand >= numFloors ? 0 : rand;
                addRequest(floor, requests);
            }
            return requests;
        }

        private void sendRequests(final ArrayList<Request> requests) {
            for (final Request request : requests) {
                final ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setSender(myAgent.getAID());
                msg.addReceiver(elevators.get(random.nextInt(elevators.size())));
                msg.setProtocol(agentType);
                msg.setContent(Integer.toString(request.getSource()));
                send(msg);
            }
        }
    }
}
