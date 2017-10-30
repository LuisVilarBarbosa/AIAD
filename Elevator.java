import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.Random;

public class Elevator extends Agent {
	private int maxWeight;  // not in use
	private int moveTime = 20;
	private int actualFloor = 0;

	private ArrayList<Integer> internalRequests = new ArrayList<>();

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
				int nextFloor = internalRequests.remove(0);
				System.out.println("Agent: " + this.getAgent().getAID() + " Floor: " + nextFloor);
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
	}
}
