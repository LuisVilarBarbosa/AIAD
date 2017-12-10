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
    private final BuildingProperties properties;
    private final ArrayList<ElevatorProperties> elevatorsProperties;
    private final ArrayList<AID> elevators;
    private final long reqGenInterval;
    private final boolean randNumRequestsPerInterval;
    private final int numRequestsPerInterval;

    public Building(final BuildingProperties properties, final long reqGenInterval, final ArrayList<ElevatorProperties> elevatorsProperties) {
        this(properties, reqGenInterval, true, 0, elevatorsProperties);
    }

    public Building(final BuildingProperties properties, final long reqGenInterval, final int numRequestsPerInterval, final ArrayList<ElevatorProperties> elevatorsProperties) {
        this(properties, reqGenInterval, false, numRequestsPerInterval, elevatorsProperties);
    }

    private Building(final BuildingProperties properties, final long reqGenInterval, final boolean randNumRequestsPerInterval, final int numRequestsPerInterval, final ArrayList<ElevatorProperties> elevatorsProperties) {
        super();
        if (reqGenInterval < 0)
            throw new IllegalArgumentException("Invalid request generation interval: " + reqGenInterval);
        if (numRequestsPerInterval < 0)
            throw new IllegalArgumentException("Invalid number of requests per interval: " + numRequestsPerInterval);
        this.properties = properties;
        this.elevatorsProperties = elevatorsProperties;
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
        final int numElevators = properties.getNumElevators();
        for (int i = 0; i < numElevators; i++)
            try {
                final Elevator elevator = new Elevator(elevatorsProperties.get(i), properties);
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

                final int numResponses = properties.getNumElevators();
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
                final int numRequests = randNumRequestsPerInterval ? MyRandom.randomInt(1, 3 * properties.getNumElevators()) : numRequestsPerInterval;
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
                final int initialFloor = MyRandom.randomFloor(properties.getNumFloors());
                if (properties.hasKeyboardOnRequest()) {
                    final int destinationFloor = MyRandom.randomFloorDifferentThan(initialFloor, properties.getNumFloors());
                    requests.add(new Request(initialFloor, destinationFloor));
                } else
                    requests.add(new Request(initialFloor));
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
