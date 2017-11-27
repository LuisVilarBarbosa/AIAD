import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Random;
import java.util.TreeSet;

public class Elevator extends Agent {
    public static String agentType = "Elevator";
	private int maxWeight;  // not in use
	private int moveTime = 20;
	private int actualFloor = 0;
	private int actualWeight = 0;
	private Random random = new Random();

    private TreeSet<Request> internalRequests = new TreeSet<>();

	public Elevator(int maxWeight) {
		super();
		if(maxWeight < 0)
		    throw new IllegalArgumentException("Invalid maximum weight: " + maxWeight);
		this.maxWeight = maxWeight;
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
	}

	private class ElevatorBehaviour extends CyclicBehaviour {

		@Override
		public void action() {
			if (!internalRequests.isEmpty()) {
				int nextFloor = getAndRemoveClosestTo(actualFloor);
				int nextActualWeight;
				int attempt = 0;
				do {
					int nPeople = 1 + random.nextInt() % (maxWeight/75);
					int newWeight = 0;
					for(int i = 0; i < nPeople; i++) {
						int n = random.nextInt() % 41;
						newWeight += (60 + n);
					}
                    int freq = random.nextInt() % 100;
					if(freq == 0)
						newWeight += 20 + random.nextInt() % 81;
					int in_out = random.nextInt() % 2;
					if(in_out == 1)
						newWeight = -newWeight;
                    nextActualWeight = actualWeight + newWeight;
					if(attempt>0)
					System.out.println(attempt);
					attempt++;
				} while(nextActualWeight < 0 || nextActualWeight > maxWeight);
				actualWeight = nextActualWeight;

				System.out.println("Agent: " + this.getAgent().getAID().getLocalName() + " Floor: " + nextFloor + " AW: " + actualWeight + " MW: " + maxWeight);
				try {
					Thread.sleep(moveTime * Math.abs(nextFloor - actualFloor));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				actualFloor = nextFloor;
			}
			ACLMessage msg;
			while((msg = receive()) != null){
				System.out.println(this.getAgent().getName() + " msg: " + msg.getContent());
				if(msg.getSender().getLocalName().startsWith(Building.agentType)) {
				    Request request = new Request(Integer.parseInt(msg.getContent()));
					if (actualFloor == request.getSource())
						request.setAttentded();
                    internalRequests.add(request);
				}
				else if(msg.getSender().getLocalName().startsWith(Elevator.agentType)) {
					String[] splittedMsg = msg.getContent().split(" ");
					if(splittedMsg.length != 2)
						System.err.println("Invalid message content.");
					int source = Integer.parseInt(splittedMsg[0]);
					int destination = Integer.parseInt(splittedMsg[1]);
					// fiquei aqui
				}
				else
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
}
