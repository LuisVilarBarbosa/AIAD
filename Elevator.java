import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetInitiator;
import jade.proto.ContractNetResponder;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

public class Elevator extends MyAgent {
    public static final String agentType = "Elevator";
    private final ElevatorProperties properties;
    private final BuildingProperties buildingProperties;
    private final ElevatorState state;
    private final ArrayList<Request> internalRequests;
    private final ElevatorStatistics statistics;
    private final long startupTime;
    private int numResponders;

    public Elevator(final ElevatorProperties properties, final BuildingProperties buildingProperties) {
        super();
        this.properties = properties;
        this.buildingProperties = buildingProperties;
        this.state = new ElevatorState();
        this.internalRequests = new ArrayList<>();
        this.statistics = new ElevatorStatistics();
        this.startupTime = System.currentTimeMillis();
        this.numResponders = 0;
    }

    @Override
    public void setup() {
        super.setup();
        registerOnDFService(agentType);
        addBehaviour(new ElevatorBehaviour());
        if (buildingProperties.isElevatorsNegotiationAllowed())
            addBehaviour(new ContractNetActivatorBehaviour());
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

            try {
                switch (fsm1State) {
                    case 0:
                        peopleEntrance();
                        if (cyclePos >= internalRequests.size()) {
                            fsm1State = (fsm1State + 1) % 4;
                            cyclePos = 0;
                        }
                        break;
                    case 1:
                        final int currentFloor = state.getCurrentFloor();
                        final int nextFloorToStop = getClosestTo(currentFloor);
                        final int diff = nextFloorToStop - currentFloor;
                        state.setMovementState(diff > 0 ? ElevatorState.GOING_UP : (diff < 0 ? ElevatorState.GOING_DOWN : ElevatorState.STOPPED));
                        updateInterface(nextFloorToStop);
                        fsm1State = (fsm1State + 1) % 4;
                        break;
                    case 2:
                        if (state.getMovementState() != ElevatorState.STOPPED)
                            moveOneFloor();
                        fsm1State = (fsm1State + 1) % 4;
                        break;
                    case 3:
                        peopleExit();
                        if (cyclePos >= internalRequests.size()) {
                            fsm1State = (fsm1State + 1) % 4;
                            cyclePos = 0;
                        }
                        break;
                    default:
                        throw new IllegalStateException("Bug on action()");
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
                System.exit(MyBoot.exitCodeOnError);
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
                            if (newWeight == 0) {
                                display("Unable to attend one request for now (too much load)");
                                cyclePos++;
                            } else {
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
                            if (!buildingProperties.hasKeyboardForRequest())
                                request.setDestinationFloor(MyRandom.randomFloorDifferentThan(request.getInitialFloor(), buildingProperties.getNumFloors()));
                            updateInterface();
                            cyclePos++;
                            fsm2State = (fsm2State + 1) % 2;
                            break;
                        default:
                            throw new IllegalStateException("Bug on peopleEntrance()");
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
                } else if (generateWeight() != 0) { // If it is possible to attend the request
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
            if (currentWeight + minNewWeight > maxWeight)
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
                    throw new IllegalStateException("Bug on moveOneFloor()");
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
                            throw new IllegalStateException("Bug on peopleExit()");
                    }
                    fsm2State = (fsm2State + 1) % 2;
                    break;
                } else
                    cyclePos++;
            }
        }
    }

    private class ContractNetActivatorBehaviour extends CyclicBehaviour {
        private long blockEnd = System.currentTimeMillis();

        @Override
        public void action() {
            final long currentMillis = System.currentTimeMillis();
            if (blockEnd >= currentMillis) {
                blockBehaviour(blockEnd - currentMillis, this);
                return;
            }

            proposeRequestToOthers();
            blockEnd = System.currentTimeMillis() + timeout;
            blockBehaviour(timeout, this);
        }

        private void proposeRequestToOthers() {
            if (!internalRequests.isEmpty()) {
                final ACLMessage aclMessage = new ACLMessage(ACLMessage.CFP);
                aclMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                aclMessage.setReplyByDate(new Date(System.currentTimeMillis() + timeout));
                aclMessage.setSender(myAgent.getAID());
                DFAgentDescription[] dfAgentDescriptions = searchOnDFService(Elevator.agentType);
                numResponders = dfAgentDescriptions.length - 1;
                for (DFAgentDescription dfAgentDescription : dfAgentDescriptions)
                    if (!dfAgentDescription.getName().equals(myAgent.getAID()))
                        aclMessage.addReceiver(dfAgentDescription.getName());

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

            @Override
            protected void handlePropose(ACLMessage propose, Vector v) {
                super.handlePropose(propose, v);
                display("Agent " + propose.getSender().getLocalName() + " proposed " + propose.getContent());
            }

            @Override
            protected void handleRefuse(ACLMessage refuse) {
                super.handleRefuse(refuse);
                display("Agent " + refuse.getSender().getLocalName() + " refused");
            }

            @Override
            protected void handleFailure(ACLMessage failure) {
                super.handleFailure(failure);
                if (failure.getSender().equals(myAgent.getAMS())) {
                    // FAILURE notification from the JADE runtime: the receiver
                    // does not exist
                    display("Responder does not exist");
                } else
                    display("Agent " + failure.getSender().getLocalName() + " failed");
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void handleAllResponses(Vector responses, Vector acceptances) {
                super.handleAllResponses(responses, acceptances);

                if (responses.size() < numResponders) {
                    // Some responder didn't reply within the specified timeout
                    display("Timeout expired: missing " + (numResponders - responses.size()) + " responses from " + numResponders + " expected");
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
                        final Request request = internalRequests.get(i);
                        if (request.getInitialFloor() == acceptedRequest.getInitialFloor() && request.getDestinationFloor() == acceptedRequest.getDestinationFloor() && !request.isAttended()) {
                            display("Accepting proposal " + bestProposal + " from responder " + bestProposer.getLocalName());
                            accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            statistics.setAcceptProposalsSent(statistics.getAcceptProposalsSent() + 1);
                            break;
                        } else
                            i++;
                    }
                }
            }

            @Override
            protected void handleInform(ACLMessage inform) {
                super.handleInform(inform);
                final MessageContent messageContent = new MessageContent(inform.getContent());
                for (int i = 0; i < internalRequests.size(); i++) {
                    final Request request = internalRequests.get(i);
                    if (request.getInitialFloor() == messageContent.getInitialFloor() && request.getDestinationFloor() == messageContent.getDestinationFloor()) {
                        display(inform.getSender().getLocalName() + " informed that received the accepted proposal message");
                        internalRequests.remove(i);
                        break;
                    }
                }
            }
        });
    }

    private void setupContractNetResponderBehaviour() {
        final MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET)
        );
        addBehaviour(new ContractNetResponder(this, template) {

            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, FailureException, RefuseException {
                super.handleCfp(cfp);

                if (cfp.getReplyByDate().before(new Date()))
                    return null;

                display(cfp.getSender().getLocalName() + " sent action " + cfp.getContent());
                final MessageContent proposedRequest = new MessageContent(cfp.getContent());
                final ACLMessage propose = cfp.createReply();
                long myTimeToInitialFloor = expectedTimeToFloor(proposedRequest.getInitialFloor());
                if (myTimeToInitialFloor <= proposedRequest.getTimeToInitialFloor()) {
                    display(cfp.getSender().getLocalName() + " sent request proposed with " + myTimeToInitialFloor);
                    propose.setPerformative(ACLMessage.PROPOSE);
                    final MessageContent myPropose = new MessageContent(proposedRequest.getInitialFloor(), proposedRequest.getDestinationFloor(), myTimeToInitialFloor);
                    propose.setContent(myPropose.toString());
                    statistics.setProposesSent(statistics.getProposesSent() + 1);
                } else {
                    display(cfp.getSender().getLocalName() + " sent request refused");
                    propose.setPerformative(ACLMessage.REFUSE);
                    statistics.setRefusesSent(statistics.getRefusesSent() + 1);
                }
                return propose;
            }

            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
                super.handleAcceptProposal(cfp, propose, accept);
                final MessageContent messageContent = new MessageContent(cfp.getContent());
                internalRequests.add(new Request(messageContent.getInitialFloor(), messageContent.getDestinationFloor()));
                statistics.setAcceptProposalsReceived(statistics.getAcceptProposalsReceived() + 1);

                final ACLMessage inform = accept.createReply();
                inform.setPerformative(ACLMessage.INFORM);
                inform.setContent(cfp.getContent());
                display(cfp.getSender().getLocalName() + " sent request added");
                return inform;
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
        return state.getCurrentFloor() +
                MyInterface.separator + state.getCurrentWeight() +
                MyInterface.separator + internalRequests.size() +
                MyInterface.separator + ElevatorState.getMovementStateString(state.getMovementState()) +
                MyInterface.separator + nextFloorToStop +
                MyInterface.separator + state.getNumPeople() +
                MyInterface.separator + statistics.getCFPsSent() +
                MyInterface.separator + statistics.getProposesSent() +
                MyInterface.separator + statistics.getRefusesSent() +
                MyInterface.separator + statistics.getAcceptProposalsSent() +
                MyInterface.separator + statistics.getAcceptProposalsReceived() +
                MyInterface.separator + statistics.getPeopleEntranceTime() +
                MyInterface.separator + statistics.getPeopleExitTime() +
                MyInterface.separator + statistics.getMinWaitTime() +
                MyInterface.separator + statistics.getMaxWaitTime() +
                MyInterface.separator + statistics.getUptime() +
                MyInterface.separator + statistics.getDowntime() +
                MyInterface.separator + statistics.getUseRate() +
                MyInterface.separator + properties.getMaxWeight() +
                MyInterface.separator + properties.getMovementTime() +
                MyInterface.separator + properties.getPersonEntranceTime() +
                MyInterface.separator + properties.getPersonExitTime();
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
