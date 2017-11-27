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

import java.util.Enumeration;
import java.util.Random;
import java.util.TreeSet;
import java.util.Vector;

public class Elevator extends Agent {
    public static String agentType = "Elevator";
    private int maxWeight;
    private int numFloors;
    private int moveTime = 20;
    private int actualFloor = 0;
    private int actualWeight = 0;
    private Random random = new Random();
    private TreeSet<Request> internalRequests = new TreeSet<>();
    private int nResponders = 3;    // to remove

    public Elevator(int maxWeight, int numFloors) {
        super();
        if (maxWeight < 0)
            throw new IllegalArgumentException("Invalid maximum weight: " + maxWeight);
        this.maxWeight = maxWeight;
        this.numFloors = numFloors;
    }

    public void setup() {
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
            ACLMessage aclMessage = new ACLMessage(ACLMessage.INFORM);
            aclMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
            aclMessage.setSender(myAgent.getAID());
            for (int i = 0; i < 3; i++)
                aclMessage.addReceiver(myAgent.getAID(Elevator.agentType + i));
            aclMessage.setContent("5");
            setupContractNetInitiatorBehaviour(aclMessage);

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
                        //System.out.println(attempt);
                        attempt++;
                } while (nextActualWeight < 0 || nextActualWeight > maxWeight);
                actualWeight = nextActualWeight;

                //System.out.println("Agent: " + this.getAgent().getAID().getLocalName() + " Floor: " + nextFloor + " AW: " + actualWeight + " MW: " + maxWeight);
                try {
                    Thread.sleep(moveTime * Math.abs(nextFloor - actualFloor));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                actualFloor = nextFloor;
            }
            ACLMessage msg;
            while ((msg = receive(MessageTemplate.MatchProtocol(Building.agentType))) != null) {
                System.out.println(this.getAgent().getName() + " msg: " + msg.getContent());
                if (msg.getSender().getLocalName().startsWith(Building.agentType)) {
                    Request request = new Request(Integer.parseInt(msg.getContent()));
                    if (actualFloor == request.getSource())
                        request.setAttended();
                    internalRequests.add(request);
                } else
                    System.err.println("Invalid agent.");
            }
        }

        private int getAndRemoveClosestTo(int number) {
            Request floor = internalRequests.floor(new Request(number - 1));
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
                System.out.println("Agent " + propose.getSender().getName() + " proposed " + propose.getContent());
            }

            protected void handleRefuse(ACLMessage refuse) {
                System.out.println("Agent " + refuse.getSender().getName() + " refused");
            }

            protected void handleFailure(ACLMessage failure) {
                if (failure.getSender().equals(myAgent.getAMS())) {
                    // FAILURE notification from the JADE runtime: the receiver
                    // does not exist
                    System.out.println("Responder does not exist");
                } else {
                    System.out.println("Agent " + failure.getSender().getName() + " failed");
                }
                // Immediate failure --> we will not receive a response from this agent
                nResponders--;
            }

            protected void handleAllResponses(Vector responses, Vector acceptances) {
                if (responses.size() < nResponders) {
                    // Some responder didn't reply within the specified timeout
                    System.out.println("Timeout expired: missing " + (nResponders - responses.size()) + " responses");
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
                        int proposal = Integer.parseInt(msg.getContent());
                        if (proposal > bestProposal) {
                            bestProposal = proposal;
                            bestProposer = msg.getSender();
                            accept = reply;
                        }
                    }
                }
                // Accept the proposal of the best proposer
                if (accept != null) {
                    System.out.println("Accepting proposal " + bestProposal + " from responder " + bestProposer.getName());
                    accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                }
            }

            protected void handleInform(ACLMessage inform) {
                System.out.println("Agent " + inform.getSender().getName() + " successfully performed the requested action");
            }
        });
    }

    private void setupContractNetResponderBehaviour() {
        MessageTemplate template = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        addBehaviour(new ContractNetResponder(this, template) {

            protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
                System.out.println("Agent " + getLocalName() + ": CFP received from " + cfp.getSender().getName() + ". Action is " + cfp.getContent());
                Request request = new Request(cfp.getContent());
                int newSourceFloor = request.getSource();
                double proposal = Math.abs(actualFloor - newSourceFloor) / (double) numFloors;
                if (proposal <= 0.25) {
                    // We provide a proposal
                    System.out.println("Agent " + getLocalName() + ": Proposing " + proposal);
                    ACLMessage propose = cfp.createReply();
                    propose.setPerformative(ACLMessage.PROPOSE);
                    propose.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                    propose.setContent(String.valueOf(newSourceFloor));
                    return propose;
                } else {
                    // We refuse to provide a proposal
                    System.out.println("Agent " + getLocalName() + ": Refuse");
                    return null;
                    //throw new RefuseException("evaluation-failed");
                }

            }

            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
                System.out.println("Agent " + getLocalName() + ": Proposal accepted");
                if (performAction()) {
                    System.out.println("Agent " + getLocalName() + ": Action successfully performed");
                    ACLMessage inform = accept.createReply();
                    inform.setPerformative(ACLMessage.INFORM);
                    return inform;
                } else {
                    System.out.println("Agent " + getLocalName() + ": Action execution failed");
                    throw new FailureException("unexpected-error");
                }
            }

            protected void handleRejectProposal(ACLMessage reject) {
                System.out.println("Agent " + getLocalName() + ": Proposal rejected");
            }
        });
    }

    private boolean performAction() {
        // Simulate action execution by generating a random number
        return (Math.random() > 0.2);
    }
}
