import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;

public class Elevator extends Agent {
	private int maxWeight;  // not in use
	private int moveTime = 20;
	private int actualFloor = 0;
	private int actualWeight = 0;
	private Random random = new Random();

	private TreeSet<Integer> internalRequests = new TreeSet<>();

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
		sd.setType("Agent");
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
				int attempt = 0;
				do {
					int freq = random.nextInt() % 100;
					int nPeople = 1 + random.nextInt() % (maxWeight/75);
					int newWeight = 0;
					for(int i = 0; i < nPeople; i++) {
						int n = random.nextInt() % 41;
						newWeight += (60 + n);
					}
					if(freq == 0)
						newWeight += 20 + random.nextInt() % 80;
					int in_out = random.nextInt() % 2;
					if(in_out == 1)
						newWeight = -newWeight;
					actualWeight += newWeight;
					if(attempt>0)
					System.out.println(attempt);
					attempt++;
				} while(actualWeight < 0 || actualWeight > maxWeight);
				
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
				internalRequests.add(Integer.parseInt(msg.getContent()));
			}
		}

        private int getAndRemoveClosestTo(int number) {
            Integer floor = internalRequests.floor(number - 1);
            Integer higher = internalRequests.higher(number);

            Integer closest;
            if (floor == null)
                closest = higher;
            else if (higher == null)
                closest = floor;
            else if (number - floor < higher - number)
                closest = floor;
            else
                closest = higher;

            if (closest == null)
                return number;
            else {
                internalRequests.remove(closest);
                return closest;
            }
        }
	}
}
