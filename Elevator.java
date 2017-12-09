import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetInitiator;
import jade.proto.ContractNetResponder;

import javax.management.timer.Timer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

public class Elevator extends MyAgent {
    public static final String agentType = "Elevator";
    private final ElevatorProperties properties;
    private final ElevatorState state;
    private ArrayList<Request> internalRequests;
    private final ElevatorStatistics statistics;
    private final long startupTime;
    private final int numElevators;

    public Elevator(final ElevatorProperties properties, final int numElevators) {
        super();
        this.properties = properties;
        this.state = new ElevatorState();
        this.internalRequests = new ArrayList<>();
        this.statistics = new ElevatorStatistics();
        this.startupTime = System.currentTimeMillis();
        this.numElevators = numElevators;
    }

    @Override
    public void setup() {
        super.setup();
        registerOnDFService(agentType);
        addBehaviour(new ElevatorBehaviour());
        setupContractNetResponderBehaviour();
        updateInterface();
    }

    private class ElevatorBehaviour extends CyclicBehaviour {
        private int fsm1State = 0;
        private int fsm2State = 0;
        private int cyclePos = 0;
        private long blockStart = System.currentTimeMillis();
        private long blockEnd = blockStart;
        private int newWeight = 0;

        @Override
        public void action() {
            final long currentMillis = System.currentTimeMillis();
            if (blockEnd >= currentMillis) {
                blockBehaviour(blockEnd - currentMillis, this);
                return;
            }

            switch (fsm1State) {
                case 0:
                    receiveRequests();
                    proposeRequestToOthers();
                    fsm1State = (fsm1State + 1) % 5;
                    break;
                case 1:
                    peopleEntrance();
                    if (cyclePos >= internalRequests.size()) {
                        fsm1State = (fsm1State + 1) % 5;
                        cyclePos = 0;
                    }
                    break;
                case 2:
                    final int currentFloor = state.getCurrentFloor();
                    final int nextFloorToStop = getClosestTo(currentFloor);
                    final int diff = nextFloorToStop - currentFloor;
                    state.setMovementState(diff > 0 ? ElevatorState.GOING_UP : (diff < 0 ? ElevatorState.GOING_DOWN : ElevatorState.STOPPED));
                    updateInterface(nextFloorToStop);
                    fsm1State = (fsm1State + 1) % 5;
                    break;
                case 3:
                    if (state.getMovementState() != ElevatorState.STOPPED)
                        moveOneFloor();
                    fsm1State = (fsm1State + 1) % 5;
                    break;
                case 4:
                    peopleExit();
                    if (cyclePos >= internalRequests.size()) {
                        fsm1State = (fsm1State + 1) % 5;
                        cyclePos = 0;
                    }
                    break;
                default:
                    displayError("Bug on action()");
                    break;
            }
        }

        private void peopleEntrance() {
            while (cyclePos < internalRequests.size()) {
                final Request request = internalRequests.get(cyclePos);
                if (request.getInitialFloor() == state.getCurrentFloor() && !request.isAttended()) {
                    switch (fsm2State) {
                        case 0:
                            state.setMovementState(ElevatorState.STOPPED);
                            newWeight = generateWeight();
                            if (newWeight == 0)
                                cyclePos++; // unable to attend request
                            else {
                                blockStart = System.currentTimeMillis();
                                final long waitTime = blockStart - request.getCreationTime();
                                if (waitTime < statistics.getMinWaitTime())
                                    statistics.setMinWaitTime(waitTime);
                                if (waitTime > statistics.getMaxWaitTime())
                                    statistics.setMaxWaitTime(waitTime);
                                final long personEntranceTime = properties.getPersonEntranceTime();
                                blockEnd = blockStart + personEntranceTime;
                                fsm2State = (fsm2State + 1) % 2;
                                blockBehaviour(personEntranceTime, this);
                            }
                            break;
                        case 1:
                            statistics.setPeopleEntranceTime(statistics.getPeopleEntranceTime() + System.currentTimeMillis() - blockStart);
                            state.setNumPeople(state.getNumPeople() + 1);
                            request.setAttended(newWeight);
                            updateWeight();
                            if (!properties.hasKeyboardOnRequest())
                                request.setDestinationFloor(MyRandom.randomFloorDifferentThan(request.getInitialFloor(), properties.getNumFloors()));
                            updateInterface();
                            cyclePos++;
                            fsm2State = (fsm2State + 1) % 2;
                            break;
                        default:
                            displayError("Bug on peopleEntrance()");
                    }
                    break;
                } else
                    cyclePos++;
            }
        }

        private int getClosestTo(final int number) {
            int closestRequestFloor = Integer.MIN_VALUE;
            int bestDistance = Integer.MAX_VALUE;
            for (final Request request : internalRequests) {
                if (request.isAttended()) {
                    final int distance = Math.abs(number - request.getDestinationFloor());
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        closestRequestFloor = request.getDestinationFloor();
                    }
                } else {
                    final int distance = Math.abs(number - request.getInitialFloor());
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        closestRequestFloor = request.getInitialFloor();
                    }
                }
            }

            if (closestRequestFloor == Integer.MIN_VALUE)
                return number;
            else
                return closestRequestFloor;
        }

        private void updateWeight() {
            int currentWeight = 0;
            for (final Request request : internalRequests)
                if (request.isAttended())
                    currentWeight += request.getWeight();
            state.setCurrentWeight(currentWeight);
        }

        private int generateWeight() {
            final int currentWeight = state.getCurrentWeight();
            final int minNewWeight = 60;
            final int maxWeight = properties.getMaxWeight();
            if(currentWeight + minNewWeight > maxWeight)
                return 0;

            int nextCurrentWeight, newWeight;
            do {
                newWeight = MyRandom.randomInt(minNewWeight, 100);
                if (MyRandom.randomInt(0, 100) == 0)
                    newWeight += MyRandom.randomInt(20, 100);
                nextCurrentWeight = currentWeight + newWeight;
            } while (nextCurrentWeight > maxWeight);
            return newWeight;
        }

        private void moveOneFloor() {
            switch (fsm2State) {
                case 0:
                    final long movementTime = properties.getMovementTime();
                    blockStart = System.currentTimeMillis();
                    blockEnd = blockStart + movementTime;
                    blockBehaviour(movementTime, this);
                    break;
                case 1:
                    updateFloorBasedOnMovementState();
                    state.setMovementState(ElevatorState.STOPPED);
                    statistics.setUptime(statistics.getUptime() + System.currentTimeMillis() - blockStart);
                    statistics.setDowntime(System.currentTimeMillis() - startupTime - statistics.getUptime());
                    updateInterface();
                    break;
                default:
                    displayError("Bug on moveOneFloor()");
            }
            fsm2State = (fsm2State + 1) % 2;
        }

        private void updateFloorBasedOnMovementState() {
            switch (state.getMovementState()) {
                case ElevatorState.STOPPED:
                    break;
                case ElevatorState.GOING_UP:
                    state.setCurrentFloor(state.getCurrentFloor() + 1);
                    break;
                case ElevatorState.GOING_DOWN:
                    state.setCurrentFloor(state.getCurrentFloor() - 1);
                    break;
                default:
                    throw new IllegalStateException("Unexpected state");
            }
        }

        private void peopleExit() {
            while (cyclePos < internalRequests.size()) {
                final Request request = internalRequests.get(cyclePos);
                if (request.isAttended() && request.getDestinationFloor() == state.getCurrentFloor()) {
                    switch (fsm2State) {
                        case 0:
                            state.setMovementState(ElevatorState.STOPPED);
                            final long personExitTime = properties.getPersonExitTime();
                            blockStart = System.currentTimeMillis();
                            blockEnd = blockStart + personExitTime;
                            blockBehaviour(personExitTime, this);
                            break;
                        case 1:
                            internalRequests.remove(cyclePos);
                            statistics.setPeopleExitTime(statistics.getPeopleExitTime() + System.currentTimeMillis() - blockStart);
                            state.setNumPeople(state.getNumPeople() - 1);
                            updateWeight();
                            updateInterface();
                            break;
                        default:
                            displayError("Bug on peopleExit()");
                    }
                    fsm2State = (fsm2State + 1) % 2;
                    break;
                } else
                    cyclePos++;
            }
        }

        private void receiveRequests() {
            ACLMessage msg;
            while ((msg = receive(MessageTemplate.MatchProtocol(Building.agentType))) != null) {
                display(Building.agentType + " sent " + msg.getContent());
                if (msg.getSender().getLocalName().startsWith(Building.agentType)) {
                    final MessageContent messageContent = new MessageContent(msg.getContent());
                    final Request request = new Request(messageContent.getInitialFloor(), messageContent.getDestinationFloor());
                    internalRequests.add(request);
                } else
                    displayError("Invalid agent");
            }
        }

        private void proposeRequestToOthers() {
            if (!internalRequests.isEmpty()) {
                final ACLMessage aclMessage = new ACLMessage(ACLMessage.CFP);
                aclMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                aclMessage.setReplyByDate(new Date(System.currentTimeMillis() + 2 * Timer.ONE_SECOND));
                aclMessage.setSender(myAgent.getAID());
                for (int i = 0; i < numElevators; i++)
                    if (!myAgent.getAID().getLocalName().equals(Elevator.agentType + i))
                        aclMessage.addReceiver(myAgent.getAID(Elevator.agentType + i));

                final long currentTime = System.currentTimeMillis();
                long largestWaitTime = 0;
                Request requestToSend = null;
                for (Request request : internalRequests) {
                    if (!request.isAttended()) {
                        final long timeToInitialFloor = expectedTimeToFloor(request.getInitialFloor());
                        final long waitTime = currentTime - request.getCreationTime() + timeToInitialFloor;
                        if (waitTime > largestWaitTime) {
                            largestWaitTime = waitTime;
                            requestToSend = request;
                        }
                    }
                }

                if (requestToSend != null) {
                    final long timeToInitialFloor = largestWaitTime - (currentTime - requestToSend.getCreationTime());
                    final MessageContent messageContent = new MessageContent(requestToSend.getInitialFloor(), requestToSend.getDestinationFloor(), timeToInitialFloor);
                    aclMessage.setContent(messageContent.toString());
                    display(myAgent.getAID().getLocalName() + " informing " + messageContent.toString());
                    setupContractNetInitiatorBehaviour(aclMessage);
                    statistics.setCFPsSent(statistics.getCFPsSent() + 1);
                }
            }
        }
    }

    private void setupContractNetInitiatorBehaviour(ACLMessage message) {
        addBehaviour(new ContractNetInitiator(this, message) {

            protected void handlePropose(ACLMessage propose, Vector v) {
                super.handlePropose(propose, v);
                display("Agent " + propose.getSender().getLocalName() + " proposed " + propose.getContent());
            }

            protected void handleRefuse(ACLMessage refuse) {
                super.handleRefuse(refuse);
                display("Agent " + refuse.getSender().getLocalName() + " refused");
            }

            protected void handleFailure(ACLMessage failure) {
                super.handleFailure(failure);
                if (failure.getSender().equals(myAgent.getAMS())) {
                    // FAILURE notification from the JADE runtime: the receiver
                    // does not exist
                    display("Responder does not exist");
                } else
                    display("Agent " + failure.getSender().getLocalName() + " failed");
            }

            protected void handleAllResponses(Vector responses, Vector acceptances) {
                super.handleAllResponses(responses, acceptances);

                final int numResponses = numElevators - 1;
                if (responses.size() < numResponses) {
                    // Some responder didn't reply within the specified timeout
                    display("Timeout expired: missing " + (numResponses - responses.size()) + " responses from " + numResponses + " expected");
                }

                // Evaluate proposals.
                long bestProposal = Long.MAX_VALUE;
                AID bestProposer = null;
                ACLMessage accept = null;
                MessageContent acceptedRequest = null;
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
                            bestProposer = msg.getSender();
                            accept = reply;
                            acceptedRequest = proposal;
                        }
                    }
                }
                // Accept the proposal of the best proposer
                if (accept != null) {
                    for (int i = 0; i < internalRequests.size(); ) {
                        Request request = internalRequests.get(i);
                        if (request.getInitialFloor() == acceptedRequest.getInitialFloor() && request.getDestinationFloor() == acceptedRequest.getDestinationFloor() && !request.isAttended()) {
                            display("Accepting proposal " + bestProposal + " from responder " + bestProposer.getLocalName());
                            accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            internalRequests.remove(request);
                            statistics.setAcceptedProposalsSent(statistics.getAcceptedProposalsSent() + 1);
                            break;
                        } else
                            i++;
                    }
                }
            }
        });
    }

    private void setupContractNetResponderBehaviour() {
        MessageTemplate template = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP), MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET));
        addBehaviour(new ContractNetResponder(this, template) {
            protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, FailureException, RefuseException {
                super.handleCfp(cfp);

                if (cfp.getReplyByDate().before(new Date()))
                    return null;

                display(cfp.getSender().getLocalName() + " sent action " + cfp.getContent());
                MessageContent proposedRequest = new MessageContent(cfp.getContent());
                ACLMessage propose = cfp.createReply();
                propose.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                long myTimeToInitialFloor = expectedTimeToFloor(proposedRequest.getInitialFloor());
                if (myTimeToInitialFloor <= proposedRequest.getTimeToInitialFloor()) {
                    display(cfp.getSender().getLocalName() + " sent request proposed with " + myTimeToInitialFloor);
                    propose.setPerformative(ACLMessage.PROPOSE);
                    MessageContent myPropose = new MessageContent(proposedRequest.getInitialFloor(), proposedRequest.getDestinationFloor(), myTimeToInitialFloor);
                    propose.setContent(myPropose.toString());
                    statistics.setProposesSent(statistics.getProposesSent() + 1);
                } else {
                    display(cfp.getSender().getLocalName() + " sent request refused");
                    propose.setPerformative(ACLMessage.REFUSE);
                    statistics.setRefusesSent(statistics.getRefusesSent() + 1);
                }
                return propose;
            }

            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
                super.handleAcceptProposal(cfp, propose, accept);
                display(cfp.getSender().getLocalName() + " sent request added");
                MessageContent messageContent = new MessageContent(cfp.getContent());
                internalRequests.add(new Request(messageContent.getInitialFloor(), messageContent.getDestinationFloor()));
                statistics.setAcceptedProposalsReceived(statistics.getAcceptedProposalsReceived() + 1); // Includes all acceptances, not only from elevators.
                return null;
            }
        });
    }

    private void updateInterface() {
        updateInterface(state.getCurrentFloor());
    }

    private void updateInterface(int nextFloorToStop) {
        final ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setSender(this.getAID());
        msg.addReceiver(this.getAID(MyInterface.agentType));
        msg.setProtocol(MyInterface.agentType);
        msg.setContent(stateString(nextFloorToStop));
        send(msg);
    }

    private String stateString(final int nextFloorToStop) {
        final StringBuilder sb = new StringBuilder();
        sb.append(state.getCurrentFloor());
        sb.append(MyInterface.separator).append(state.getCurrentWeight());
        sb.append(MyInterface.separator).append(internalRequests.size());
        sb.append(MyInterface.separator).append(ElevatorState.getMovementStateString(state.getMovementState()));
        sb.append(MyInterface.separator).append(nextFloorToStop);
        sb.append(MyInterface.separator).append(state.getNumPeople());
        sb.append(MyInterface.separator).append(statistics.getCFPsSent());
        sb.append(MyInterface.separator).append(statistics.getProposesSent());
        sb.append(MyInterface.separator).append(statistics.getRefusesSent());
        sb.append(MyInterface.separator).append(statistics.getAcceptedProposalsSent());
        sb.append(MyInterface.separator).append(statistics.getAcceptedProposalsReceived());
        sb.append(MyInterface.separator).append(statistics.getPeopleEntranceTime());
        sb.append(MyInterface.separator).append(statistics.getPeopleExitTime());
        sb.append(MyInterface.separator).append(statistics.getMinWaitTime());
        sb.append(MyInterface.separator).append(statistics.getMaxWaitTime());
        sb.append(MyInterface.separator).append(statistics.getUptime());
        sb.append(MyInterface.separator).append(statistics.getDowntime());
        sb.append(MyInterface.separator).append(statistics.getUseRate());
        sb.append(MyInterface.separator).append(properties.getMaxWeight());
        sb.append(MyInterface.separator).append(properties.getMovementTime());
        sb.append(MyInterface.separator).append(properties.getPersonEntranceTime());
        sb.append(MyInterface.separator).append(properties.getPersonExitTime());
        sb.append(MyInterface.separator).append(ElevatorProperties.getHasKeyboardOnRequestString(properties.hasKeyboardOnRequest()));
        return sb.toString();
    }

    private long expectedTimeToFloor(int floor) {
        final int currentFloor = state.getCurrentFloor();
        long time = properties.getMovementTime() * Math.abs(floor - currentFloor);
        for (Request request : internalRequests) {
            if (request.isAttended() && isBetween(floor, currentFloor, request.getDestinationFloor()))
                time += properties.getPersonExitTime();
            else if (isBetween(floor, currentFloor, request.getInitialFloor()))
                time += properties.getPersonEntranceTime();
        }
        return time;
    }

    private boolean isBetween(int number, int v1, int v2) {
        if (v1 <= v2)
            return v1 < number && number < v2;
        else
            return v2 < number && number < v1;
    }
}
