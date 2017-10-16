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
	private int maxPeople = 8;  // not in use
	private int moveTime = 20;
	private int actualFloor = 0;
	private Random random = new Random();
	private Building building;

	private ArrayList<Integer> internalRequests = new ArrayList<>();

	public Elevator(Building building) {
		super();
		this.building = building;
	}

	public void setup() {
		this.addBehaviour(new ElevatorBehaviour());
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setName(getName());
		sd.setType("Agente ");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			// TODO Auto-generated catch block
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
			ACLMessage msg = receive();
			if(msg != null) {
				System.out.println(this.getAgent().getName() + " msg: " + msg.getContent());
				int n = random.nextInt() % 50;
				for (int i = 0; i < n; i++) {
					int rand = random.nextInt() % (building.getMaxFloor() - building.getMinFloor()) - building.getMinFloor();
					if (rand > building.getMaxFloor())
						internalRequests.add(0);
					else
						internalRequests.add(rand);
				}
			}
		}
	}
}
