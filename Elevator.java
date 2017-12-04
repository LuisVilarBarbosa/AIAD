import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetInitiator;
import jade.proto.ContractNetResponder;

import javax.management.timer.Timer;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class Elevator extends Agent {
    public static final String agentType = "Elevator";
    private final ElevatorProperties properties;
    private final ElevatorState state;
    private final Random random;
    private ArrayList<Request> internalRequests;
    private final ConcurrentSkipListMap<Long, String> information;
    private final ElevatorStatistics statistics;
    private final int nResponders = 3;    // to remove

    public Elevator(final ElevatorProperties properties) {
        super();
        this.properties = properties;
        this.state = new ElevatorState();
        this.random = new Random();
        this.internalRequests = new ArrayList<>();
        this.information = new ConcurrentSkipListMap<>();
        this.statistics = new ElevatorStatistics();
    }

    public void setup() {
        super.setup();
        addBehaviour(new ElevatorBehaviour());
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setName(getName());
        sd.setType(agentType);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        setupContractNetResponderBehaviour();
        updateInterface();
    }

    private class ElevatorBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            receiveRequests();
            proposeRequestToOthers();
            peopleEntrance();
            final int currentFloor = state.getCurrentFloor();
            final int nextFloorToStop = getClosestTo(currentFloor);
            final int diff = nextFloorToStop - currentFloor;
            state.setMovementState(diff > 0 ? ElevatorState.GOING_UP : (diff < 0 ? ElevatorState.GOING_DOWN : ElevatorState.STOPPED));
            updateInterface(nextFloorToStop);
            //addToInformation("Agent: " + this.getAgent().getAID().getLocalName() + " Floor: " + nextFloor + " AW: " + actualWeight + " MW: " + maxWeight);
            if (state.getMovementState() != ElevatorState.STOPPED)
                CommonFunctions.sleep(properties.getMovementTime());
            updateFloorBasedOnMovementState();
            peopleExit();
            if (internalRequests.isEmpty()) {
                state.setCurrentWeight(0);
                state.setNumPeople(0);
                updateInterface();
            }
        }

        private void peopleEntrance() {
            state.setMovementState(ElevatorState.STOPPED);
            int newPeople = 0;
            for (Request request : internalRequests) {
                if (request.getInitialFloor() == state.getCurrentFloor()) {
                    final long begin = System.currentTimeMillis();
                    long waitTime = begin - request.getCreationTime();
                    if (waitTime < statistics.getMinWaitTime())
                        statistics.setMinWaitTime(waitTime);
                    if (waitTime > statistics.getMaxWaitTime())
                        statistics.setMaxWaitTime(waitTime);
                    CommonFunctions.sleep(Timer.ONE_SECOND);// entrance time
                    statistics.setPeopleEntranceTime(statistics.getPeopleEntranceTime() + System.currentTimeMillis() - begin);
                    newPeople++;
                    state.setNumPeople(state.getNumPeople() + 1);
                    request.setAttended();
                    request.setDestinationFloor(random.nextInt(properties.getNumFloors()));
                    updateInterface();
                }
            }
            updateWeight(newPeople);    // could be one by one - should be in peopleEntrance() and peopleExit() only
        }

        private int getClosestTo(final int number) {
            int closestRequestFloor = Integer.MIN_VALUE;
            int bestDistance = Integer.MAX_VALUE;
            for (Request request : internalRequests) {
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

        private void updateWeight(final int numPeople) {
            int nextCurrentWeight;
            do {
                int newWeight = 0;
                for (int i = 0; i < numPeople; i++) {
                    int n = random.nextInt() % 41;
                    newWeight += (60 + n);
                }
                int freq = random.nextInt() % 100;
                if (freq == 0)
                    newWeight += 20 + random.nextInt() % 81;
                int in_out = random.nextInt() % 2;
                if (in_out == 1)
                    newWeight = -newWeight;
                nextCurrentWeight = state.getCurrentWeight() + newWeight;
            } while (nextCurrentWeight < 0 || nextCurrentWeight > properties.getMaxWeight());
            state.setCurrentWeight(nextCurrentWeight);
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
            state.setMovementState(ElevatorState.STOPPED);
            final ArrayList<Request> newInternalRequests = new ArrayList<>(internalRequests.size());
            for (Request request : internalRequests) {
                if (request.isAttended() && request.getDestinationFloor() == state.getCurrentFloor()) {
                    final long begin = System.currentTimeMillis();
                    CommonFunctions.sleep(Timer.ONE_SECOND);    // exit time
                    statistics.setPeopleExitTime(statistics.getPeopleExitTime() + System.currentTimeMillis() - begin);
                    state.setNumPeople(state.getNumPeople() - 1);
                    updateInterface();
                } else
                    newInternalRequests.add(request);
            }
            internalRequests = newInternalRequests;
            updateInterface();
        }

        private void receiveRequests() {
            ACLMessage msg;
            while ((msg = receive(MessageTemplate.MatchProtocol(Building.agentType))) != null) {
                addToInformation(Building.agentType + " sent " + msg.getContent());
                if (msg.getSender().getLocalName().startsWith(Building.agentType)) {
                    Request request = new Request(Integer.parseInt(msg.getContent()));
                    internalRequests.add(request);
                } else
                    addToInformation("Invalid agent");
            }
        }

        private void proposeRequestToOthers() {
            if (!internalRequests.isEmpty()) {
                // TODO While setting the negotiation we need to implement a way to temporarily lock the request, so this is not attended.
                final ACLMessage aclMessage = new ACLMessage(ACLMessage.CFP);
                aclMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                aclMessage.setReplyByDate(new Date(System.currentTimeMillis() + Timer.ONE_SECOND));
                aclMessage.setSender(myAgent.getAID());
                for (int i = 0; i < nResponders; i++)
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
                    final ElevatorMessage elevatorMessage = new ElevatorMessage(requestToSend.getInitialFloor(), requestToSend.getDestinationFloor(), timeToInitialFloor);
                    aclMessage.setContent(elevatorMessage.toString());
                    addToInformation(myAgent.getAID().getLocalName() + " informing " + elevatorMessage.toString());
                    setupContractNetInitiatorBehaviour(aclMessage);
                }
            }
        }
    }

    private void setupContractNetInitiatorBehaviour(ACLMessage message) {
        addBehaviour(new ContractNetInitiator(this, message) {

            protected void handlePropose(ACLMessage propose, Vector v) {
                addToInformation("Agent " + propose.getSender().getLocalName() + " proposed " + propose.getContent());
            }

            protected void handleRefuse(ACLMessage refuse) {
                addToInformation("Agent " + refuse.getSender().getLocalName() + " refused");
            }

            protected void handleFailure(ACLMessage failure) {
                if (failure.getSender().equals(myAgent.getAMS())) {
                    // FAILURE notification from the JADE runtime: the receiver
                    // does not exist
                    addToInformation("Responder does not exist");
                } else
                    addToInformation("Agent " + failure.getSender().getLocalName() + " failed");
            }

            protected void handleAllResponses(Vector responses, Vector acceptances) {
                if (responses.size() < nResponders) {
                    // Some responder didn't reply within the specified timeout
                    addToInformation("Timeout expired: missing " + (nResponders - responses.size()) + " responses");
                }
                // Evaluate proposals.
                long bestProposal = -1;
                AID bestProposer = null;
                ACLMessage accept = null;
                ElevatorMessage acceptedRequest = null;
                Enumeration e = responses.elements();
                while (e.hasMoreElements()) {
                    ACLMessage msg = (ACLMessage) e.nextElement();
                    if (msg.getPerformative() == ACLMessage.PROPOSE) {
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        acceptances.addElement(reply);
                        ElevatorMessage proposal = new ElevatorMessage(msg.getContent());
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
                    addToInformation("Accepting proposal " + bestProposal + " from responder " + bestProposer.getLocalName());
                    accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    internalRequests.remove(new Request(acceptedRequest.getInitialFloor()));
                }
            }
        });
    }

    private void setupContractNetResponderBehaviour() {
        MessageTemplate template = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        addBehaviour(new ContractNetResponder(this, template) {

            protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
                addToInformation(cfp.getSender().getLocalName() + "sent action " + cfp.getContent());
                ElevatorMessage proposedRequest = new ElevatorMessage(cfp.getContent());
                ACLMessage propose = cfp.createReply();
                propose.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                long myTimeToInitialFloor = expectedTimeToFloor(proposedRequest.getInitialFloor());
                if (myTimeToInitialFloor <= proposedRequest.getTimeToInitialFloor()) {
                    addToInformation(cfp.getSender().getLocalName() + " sent request proposed with " + myTimeToInitialFloor);
                    propose.setPerformative(ACLMessage.PROPOSE);
                    ElevatorMessage myPropose = new ElevatorMessage(proposedRequest.getInitialFloor(), proposedRequest.getDestinationFloor(), myTimeToInitialFloor);
                    propose.setContent(myPropose.toString());
                } else {
                    addToInformation(cfp.getSender().getLocalName() + " sent request refused");
                    propose.setPerformative(ACLMessage.REFUSE);
                }
                return propose;
            }

            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
                addToInformation(cfp.getSender().getLocalName() + " sent request added");
                internalRequests.add(new Request(cfp.getContent()));
                return null;
            }
        });
    }

    private void updateInterface() {
        updateInterface(state.getCurrentFloor());
    }

    private void updateInterface(int nextFloorToStop) {
        cleanOldInformation();
        final ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setSender(this.getAID());
        msg.addReceiver(this.getAID(MyInterface.agentType));
        msg.setProtocol(MyInterface.agentType);
        msg.setContent(stateString(nextFloorToStop));
        send(msg);
    }

    private void addToInformation(final String str) {
        information.put(System.currentTimeMillis(), str);
    }

    private void cleanOldInformation() {
        for (final Long keyMillis : information.keySet())
            if (keyMillis < System.currentTimeMillis() - 10 * properties.getMovementTime())
                information.remove(keyMillis);
    }

    private String stateString(int nextFloorToStop) {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.getAID().getLocalName()).append(":\n");
        sb.append("\tFloor=").append(state.getCurrentFloor());
        sb.append(" Weight=").append(state.getCurrentWeight());
        sb.append(" NumRequests=").append(internalRequests.size());
        sb.append(" State=").append(ElevatorState.getMovementStateString(state.getMovementState())).append("\n");
        sb.append("\tNextFloorToStop=").append(nextFloorToStop);
        sb.append(" NumPeople=").append(state.getNumPeople()).append("\n");
        sb.append("\tPeopleEntranceTime=").append(statistics.getPeopleEntranceTime());
        sb.append(" PeopleExitTime=").append(statistics.getPeopleExitTime()).append("\n");
        sb.append("\tMinWaitTime=").append(statistics.getMinWaitTime());
        sb.append(" MaxWaitTime=").append(statistics.getMaxWaitTime()).append("\n");
        sb.append("\tMaxWeight=").append(properties.getMaxWeight());
        sb.append(" MovementTime=").append(properties.getMovementTime()).append("\n");
        for (final String info : information.values())
            sb.append("\t").append(info).append("\n");
        sb.append("\n");
        return sb.toString();
    }

    private long expectedTimeToFloor(int floor) {
        final int currentFloor = state.getCurrentFloor();
        long time = properties.getMovementTime() * Math.abs(floor - currentFloor);
        for (Request request : internalRequests) {
            int requestNextStopFloor;
            if (request.isAttended())
                requestNextStopFloor = request.getDestinationFloor();
            else
                requestNextStopFloor = request.getInitialFloor();
            if (isBetween(floor, currentFloor, requestNextStopFloor))
                time += Timer.ONE_SECOND;   // 1 second = entrance/exit time
        }
        return time;
    }

    private boolean isBetween(int number, int v1, int v2) {
        if (v1 <= v2)
            return v1 <= number && number <= v2;
        else
            return v2 <= number && number <= v1;
    }
}
