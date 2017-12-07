import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;
import java.util.Random;

public class Building extends MyAgent {
    public static final String agentType = "Building";
    private final int numFloors;
    private final int numElevators;
    private final ArrayList<ElevatorProperties> elevatorsProperties;
    private final Random random;
    private final ArrayList<AID> elevators;
    private final long reqGenInterval;
    private final boolean randNumRequestsPerInterval;
    private final int numRequestsPerInterval;

    public Building(final int numFloors, final long reqGenInterval, final ArrayList<ElevatorProperties> elevatorsProperties) {
        this(numFloors, reqGenInterval, true, 0, elevatorsProperties);
    }

    public Building(final int numFloors, final long reqGenInterval, final int numRequestsPerInterval, final ArrayList<ElevatorProperties> elevatorsProperties) {
        this(numFloors, reqGenInterval, false, numRequestsPerInterval, elevatorsProperties);
    }

    private Building(final int numFloors, final long reqGenInterval, final boolean randNumRequestsPerInterval, final int numRequestsPerInterval, final ArrayList<ElevatorProperties> elevatorsProperties) {
        super();
        if (numFloors < 0)
            throw new IllegalArgumentException("Invalid number of floors:" + numFloors);
        if (reqGenInterval < 0)
            throw new IllegalArgumentException("Invalid request generation interval: " + reqGenInterval);
        this.numFloors = numFloors;
        this.elevatorsProperties = elevatorsProperties;
        this.numElevators = this.elevatorsProperties.size();
        this.random = new Random();
        this.elevators = new ArrayList<>();
        this.reqGenInterval = reqGenInterval;
        this.randNumRequestsPerInterval = randNumRequestsPerInterval;
        this.numRequestsPerInterval = numRequestsPerInterval;
    }

    @Override
    protected void setup() {
        super.setup();
        registerOnDFService(agentType);
        generateElevatorsAgents();
        addBehaviour(new BuildingBehaviour());
    }

    @Override
    protected void takeDown() {
        super.takeDown();
        deregisterOnDFService();
    }

    private void generateElevatorsAgents() {
        final AgentContainer containerController = this.getContainerController();
        for (int i = 0; i < numElevators; i++)
            try {
                final Elevator elevator = new Elevator(elevatorsProperties.get(i));
                final AgentController ac = containerController.acceptNewAgent(Elevator.agentType + i, elevator);
                ac.start();
                elevators.add(elevator.getAID());
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
    }

    private class BuildingBehaviour extends CyclicBehaviour {
        private final int numRequests = randNumRequestsPerInterval ? (random.nextInt() % (3 * numElevators)) : numRequestsPerInterval;
        private long endBlock = System.currentTimeMillis();

        @Override
        public void action() {
            if (endBlock < System.currentTimeMillis()) {
                final ArrayList<Request> requests = generateNRequests(numRequests);
                sendRequests(requests);
                endBlock = System.currentTimeMillis() + reqGenInterval;
                blockBehaviour(reqGenInterval, this);
            } else
                blockBehaviour(endBlock - System.currentTimeMillis(), this);
        }

        private int generateRandomFloor() {
            final int rand = random.nextInt(numFloors * 2);
            return rand >= numFloors ? 0 : rand;
        }

        private ArrayList<Request> generateNRequests(final int n) {
            final ArrayList<Request> requests = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                final Request request = new Request(generateRandomFloor());
                requests.add(request);
            }
            return requests;
        }

        private void sendRequests(final ArrayList<Request> requests) {
            // shouldn't be here
            for (final Request request : requests) {
                int elevatorPos = random.nextInt(elevators.size());
                if (elevatorsProperties.get(elevatorPos).hasKeyboardOnRequest())
                    request.setDestinationFloor(generateRandomFloor());

                final ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setSender(myAgent.getAID());
                msg.addReceiver(elevators.get(elevatorPos));
                msg.setProtocol(agentType);
                msg.setContent(Integer.toString(request.getInitialFloor()));
                send(msg);
            }
        }
    }
}
