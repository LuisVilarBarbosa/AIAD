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

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class Elevator extends Agent {
    public static String agentType = "Elevator";
    private final int maxWeight;
    private final int numFloors;
    private final int moveTime = 1000;
    private int actualFloor = 0;
    private int actualWeight = 0;
    private final Random random = new Random();
    private final TreeSet<Request> internalRequests = new TreeSet<>();
    private final ConcurrentSkipListMap<String, Long> information;
    private final int nResponders = 3;    // to remove

    public Elevator(int maxWeight, int numFloors) {
        super();
        if (maxWeight < 0)
            throw new IllegalArgumentException("Invalid maximum weight: " + maxWeight);
        this.maxWeight = maxWeight;
        if (numFloors < 0)
            throw new IllegalArgumentException("Invalid number of floors: " + numFloors);
        this.numFloors = numFloors;
        this.information = new ConcurrentSkipListMap<>();
    }

    public void setup() {
        super.setup();
        this.addBehaviour(new ElevatorBehaviour());
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
    }

    private class ElevatorBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            if (!internalRequests.isEmpty()) {
                int nextFloor = getAndRemoveClosestTo(actualFloor);
                int nextActualWeight;
                int attempt = 0;
                do {
                    int nPeople = 1 + random.nextInt() % (maxWeight / 75);
                    int newWeight = 0;
                    for (int i = 0; i < nPeople; i++) {
                        int n = random.nextInt() % 41;
                        newWeight += (60 + n);
                    }
                    int freq = random.nextInt() % 100;
                    if (freq == 0)
                        newWeight += 20 + random.nextInt() % 81;
                    int in_out = random.nextInt() % 2;
                    if (in_out == 1)
                        newWeight = -newWeight;
                    nextActualWeight = actualWeight + newWeight;
                    if (attempt > 0)
                        //addToInformation(attempt);
                        attempt++;
                } while (nextActualWeight < 0 || nextActualWeight > maxWeight);
                actualWeight = nextActualWeight;

                //addToInformation("Agent: " + this.getAgent().getAID().getLocalName() + " Floor: " + nextFloor + " AW: " + actualWeight + " MW: " + maxWeight);
                updateInterface();
                try {
                    Thread.sleep(moveTime * Math.abs(nextFloor - actualFloor));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                actualFloor = nextFloor;
                updateInterface();
            }
            ACLMessage msg;
            while ((msg = receive(MessageTemplate.MatchProtocol(Building.agentType))) != null) {
                addToInformation(myAgent.getName() + " msg: " + msg.getContent());
                if (msg.getSender().getLocalName().startsWith(Building.agentType)) {
                    Request request = new Request(Integer.parseInt(msg.getContent()));
                    if (actualFloor == request.getSource())
                        request.setAttended();
                    internalRequests.add(request);
                } else
                    addToInformation("Invalid agent.");
            }

            if (!internalRequests.isEmpty()) {
                // TODO While setting the negotiation we need to implement a way to temporarily lock the request, so this is not attended.
                ACLMessage aclMessage = new ACLMessage(ACLMessage.INFORM);
                aclMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                aclMessage.setSender(myAgent.getAID());
                for (int i = 0; i < nResponders; i++)
                    if (!myAgent.getAID().getLocalName().equals(Elevator.agentType + i))
                        aclMessage.addReceiver(myAgent.getAID(Elevator.agentType + i));
                Request requestToSend = internalRequests.last();
                int source = requestToSend.getSource();
                int destination = requestToSend.getDestination();
                int distanceToSource = Math.abs(((Elevator) myAgent).actualFloor - requestToSend.getSource());
                ElevatorMessage elevatorMessage = new ElevatorMessage(source, destination, distanceToSource);
                aclMessage.setContent(elevatorMessage.toString());
                addToInformation(myAgent.getAID().getLocalName() + " informing " + elevatorMessage.toString());
                setupContractNetInitiatorBehaviour(aclMessage);
            }
            if (internalRequests.isEmpty() && actualWeight != 0) {
                actualWeight = 0;
                updateInterface();
            }
        }

        private int getAndRemoveClosestTo(int number) {
            Request floor = internalRequests.floor(new Request(number));    // can find a request with 'source' equal to 'number'
            Request higher = internalRequests.higher(new Request(number));

            Request closest;
            if (floor == null)
                closest = higher;
            else if (higher == null)
                closest = floor;
            else if (number - (floor.isAttended() ? floor.getDestination() : floor.getSource()) < (higher.isAttended() ? higher.getDestination() : higher.getSource()) - number)
                closest = floor;
            else
                closest = higher;

            if (closest == null)
                return number;
            else {
                internalRequests.remove(closest);
                return closest.isAttended() ? closest.getDestination() : closest.getSource();
            }
        }
    }

    private void setupContractNetInitiatorBehaviour(ACLMessage message) {
        addBehaviour(new ContractNetInitiator(this, message) {

            protected void handlePropose(ACLMessage propose, Vector v) {
                addToInformation("Agent " + propose.getSender().getName() + " proposed " + propose.getContent());
            }

            protected void handleRefuse(ACLMessage refuse) {
                addToInformation("Agent " + refuse.getSender().getName() + " refused");
            }

            protected void handleFailure(ACLMessage failure) {
                if (failure.getSender().equals(myAgent.getAMS())) {
                    // FAILURE notification from the JADE runtime: the receiver
                    // does not exist
                    addToInformation("Responder does not exist");
                } else {
                    addToInformation("Agent " + failure.getSender().getName() + " failed");
                }
                // Immediate failure --> we will not receive a response from this agent
                //nResponders--;
            }

            protected void handleAllResponses(Vector responses, Vector acceptances) {
                if (responses.size() < nResponders) {
                    // Some responder didn't reply within the specified timeout
                    addToInformation("Timeout expired: missing " + (nResponders - responses.size()) + " responses");
                }
                // Evaluate proposals.
                int bestProposal = -1;
                AID bestProposer = null;
                ACLMessage accept = null;
                Enumeration e = responses.elements();
                while (e.hasMoreElements()) {
                    ACLMessage msg = (ACLMessage) e.nextElement();
                    if (msg.getPerformative() == ACLMessage.PROPOSE) {
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        acceptances.addElement(reply);
                        ElevatorMessage proposal = new ElevatorMessage(msg.getContent());
                        if (proposal.getDistanceToSource() <= bestProposal) {
                            bestProposal = proposal.getDistanceToSource();
                            bestProposer = msg.getSender();
                            accept = reply;
                        }
                    }
                }
                // Accept the proposal of the best proposer
                if (accept != null) {
                    addToInformation("Accepting proposal " + bestProposal + " from responder " + bestProposer.getName());
                    accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                }
            }

            protected void handleInform(ACLMessage inform) {
                addToInformation("Agent " + inform.getSender().getName() + " successfully performed the requested action");
            }
        });
    }

    private void setupContractNetResponderBehaviour() {
        MessageTemplate template = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        addBehaviour(new ContractNetResponder(this, template) {

            protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
                addToInformation("Agent " + getLocalName() + ": CFP received from " + cfp.getSender().getName() + ". Action is " + cfp.getContent());
                ElevatorMessage proposedRequest = new ElevatorMessage(cfp.getContent());
                int myDistanceToDo = Math.abs(actualFloor - proposedRequest.getSource());
                if (myDistanceToDo <= proposedRequest.getDistanceToSource()) {
                    // We provide a proposal
                    addToInformation("Agent " + getLocalName() + ": Proposing " + myDistanceToDo);
                    ACLMessage propose = cfp.createReply();
                    propose.setPerformative(ACLMessage.PROPOSE);
                    propose.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                    ElevatorMessage myPropose = new ElevatorMessage(proposedRequest.getSource(), proposedRequest.getDestination(), myDistanceToDo);
                    propose.setContent(myPropose.toString());
                    return propose;
                } else {
                    // We refuse to provide a proposal
                    addToInformation("Agent " + getLocalName() + ": Refuse");
                    return null;
                    //throw new RefuseException("evaluation-failed");
                }

            }

            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
                addToInformation("Agent " + getLocalName() + ": Proposal accepted");
                if (performAction()) {
                    addToInformation("Agent " + getLocalName() + ": Action successfully performed");
                    ACLMessage inform = accept.createReply();
                    inform.setPerformative(ACLMessage.INFORM);
                    return inform;
                } else {
                    addToInformation("Agent " + getLocalName() + ": Action execution failed");
                    throw new FailureException("unexpected-error");
                }
            }

            protected void handleRejectProposal(ACLMessage reject) {
                addToInformation("Agent " + getLocalName() + ": Proposal rejected");
            }
        });
    }

    private boolean performAction() {
        // Simulate action execution by generating a random number
        return (Math.random() > 0.2);
    }

    private void updateInterface() {
        cleanOldInformation();
        ArrayList<String> informationKeys = new ArrayList<>(information.keySet());
        ElevatorState elevatorState = new ElevatorState(actualFloor, actualWeight, internalRequests.size(), informationKeys);
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setSender(this.getAID());
        msg.addReceiver(this.getAID(MyInterface.agentType));
        msg.setProtocol(MyInterface.agentType);
        msg.setContent(elevatorState.toString());
        send(msg);
    }

    private void addToInformation(String str) {
        information.put(str, System.currentTimeMillis());
    }

    private void cleanOldInformation() {
        for (Map.Entry<String, Long> entry : information.entrySet())
            if (entry.getValue() < System.currentTimeMillis() - 10 * moveTime)
                information.remove(entry.getKey());
    }
}
