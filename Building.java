import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;

public class Building extends MyAgent {
    public static final String agentType = "Building";
    private final int numFloors;
    private final int numElevators;
    private final ArrayList<ElevatorProperties> elevatorsProperties;
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
        if (numFloors < 2)
            throw new IllegalArgumentException("Invalid number of floors: " + numFloors);
        if (reqGenInterval < 0)
            throw new IllegalArgumentException("Invalid request generation interval: " + reqGenInterval);
        this.numFloors = numFloors;
        this.elevatorsProperties = elevatorsProperties;
        this.numElevators = this.elevatorsProperties.size();
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

    private void generateElevatorsAgents() {
        final AgentContainer containerController = this.getContainerController();
        for (int i = 0; i < numElevators; i++)
            try {
                final Elevator elevator = new Elevator(elevatorsProperties.get(i), numElevators);
                final AgentController ac = containerController.acceptNewAgent(Elevator.agentType + i, elevator);
                ac.start();
                elevators.add(elevator.getAID());
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
    }

    private class BuildingBehaviour extends CyclicBehaviour {
        private long endBlock = System.currentTimeMillis();

        @Override
        public void action() {
            if (endBlock < System.currentTimeMillis()) {
                final int numRequests = randNumRequestsPerInterval ? MyRandom.randomInt(1, 3 * numElevators) : numRequestsPerInterval;
                final ArrayList<Request> requests = generateNRequests(numRequests);
                sendRequests(requests);
                endBlock = System.currentTimeMillis() + reqGenInterval;
                blockBehaviour(reqGenInterval, this);
            } else
                blockBehaviour(endBlock - System.currentTimeMillis(), this);
        }

        private ArrayList<Request> generateNRequests(final int n) {
            final ArrayList<Request> requests = new ArrayList<>();
            for (int i = 0; i < n; i++)
                requests.add(new Request(MyRandom.randomFloor(numFloors)));
            return requests;
        }

        private void sendRequests(final ArrayList<Request> requests) {
            for (final Request request : requests) {
                final int elevatorPos = MyRandom.randomInt(0, elevators.size() - 1);
                if (elevatorsProperties.get(elevatorPos).hasKeyboardOnRequest())
                    request.setDestinationFloor(MyRandom.randomFloorDifferentThan(request.getInitialFloor(), numFloors));    // shouldn't be here

                final MessageContent messageContent = new MessageContent(request.getInitialFloor(), request.getDestinationFloor(), Long.MAX_VALUE);

                final ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setSender(myAgent.getAID());
                msg.addReceiver(elevators.get(elevatorPos));
                msg.setProtocol(agentType);
                msg.setContent(messageContent.toString());
                send(msg);
            }
        }
    }
}
