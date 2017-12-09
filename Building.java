import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import javax.management.timer.Timer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

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

    private void setupContractNetInitiatorBehaviour(ACLMessage message) {
        addBehaviour(new ContractNetInitiator(this, message) {

            protected void handleAllResponses(Vector responses, Vector acceptances) {
                super.handleAllResponses(responses, acceptances);

                final int numResponses = numElevators;
                if (responses.size() < numResponses)
                    display("Timeout expired: missing " + (numResponses - responses.size()) + " responses from " + numResponses + " expected");

                // Evaluate proposals.
                long bestProposal = Long.MAX_VALUE;
                ACLMessage accept = null;
                Enumeration e = responses.elements();
                while (e.hasMoreElements()) {
                    ACLMessage msg = (ACLMessage) e.nextElement();
                    if (msg.getPerformative() == ACLMessage.PROPOSE) {
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        acceptances.addElement(reply);
                        MessageContent proposal = new MessageContent(msg.getContent());
                        if (proposal.getTimeToInitialFloor() <= bestProposal) {
                            bestProposal = proposal.getTimeToInitialFloor();
                            accept = reply;
                        }
                    }
                }

                if (accept != null)
                    accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            }
        });
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
            for (int i = 0; i < n; i++) {
                // If the elevator does not have a keyboard when creating the request, the destination floor can/will be replaced
                final int initialFloor = MyRandom.randomFloor(numFloors);
                final int destinationFloor = MyRandom.randomFloorDifferentThan(initialFloor, numFloors);
                requests.add(new Request(initialFloor, destinationFloor));
            }
            return requests;
        }

        private void sendRequests(final ArrayList<Request> requests) {
            for (final Request request : requests) {
                final MessageContent messageContent = new MessageContent(request.getInitialFloor(), request.getDestinationFloor(), Long.MAX_VALUE);
                final ACLMessage msg = new ACLMessage(ACLMessage.CFP);
                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                msg.setReplyByDate(new Date(System.currentTimeMillis() + 2 * Timer.ONE_SECOND));
                msg.setSender(myAgent.getAID());
                for (AID elevatorAID : elevators)
                    msg.addReceiver(elevatorAID);
                msg.setContent(messageContent.toString());
                setupContractNetInitiatorBehaviour(msg);
            }
        }
    }
}
