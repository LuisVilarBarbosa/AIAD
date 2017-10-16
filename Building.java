import java.util.ArrayList;
import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class Building extends Agent {
	// floor 0 must exist
	private static int minFloor = -3;
	private static int maxFloor = 20;
	private static int nElevators = 3;
	private Random random = new Random();

	private ArrayList<String> elevators = new ArrayList();
	private ArrayList<Integer> externalRequests = new ArrayList<>();
	
	private ArrayList<DFAgentDescription> descriptions = new ArrayList();

	public void setup() {
		DFAgentDescription template = new DFAgentDescription();
		Behaviour b = new SubscriptionInitiator(
				this, DFService.createSubscriptionMessage(this, getDefaultDF(), template, null)) {
			protected void handleInform(ACLMessage inform) {
				try {
					DFAgentDescription[] dfds = DFService.decodeNotification(inform.getContent());
					for(DFAgentDescription d: dfds) {
						descriptions.add(d);
					System.out.println("OI: " + d);
					}
					
				} catch (FIPAException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		};
		addBehaviour(b);
		for(int i = 0; i < nElevators; i++)
			try {
				AgentController ac = this.getContainerController().acceptNewAgent("Elevator"+i, new Elevator(this));
				ac.start();
				elevators.add(ac.getName());
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}

		this.addBehaviour(new BuildingBehaviour());
	}

	public static int getMinFloor() {
		return minFloor;
	}
	public static int getMaxFloor() {
		return maxFloor;
	}

	public void addRequest(int floor) {
		if (floor < minFloor || floor > maxFloor)
			throw new IllegalArgumentException("Invalid floor.");
		externalRequests.add(floor);
	}

	private class BuildingBehaviour extends CyclicBehaviour {

		@Override
		public void action() {
			int n = random.nextInt() % 50;
			for (int i = 0; i < n; i++) {
				int rand = Math.abs(random.nextInt() % ((maxFloor - minFloor) * 2)) - minFloor;
				if (rand > maxFloor)
					addRequest(0);
				else
					addRequest(rand);
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				for(int j = 0; j < nElevators; j++)
					//msg.addReceiver();
				msg.setContent(Integer.toString(123456));
				send(msg);
			}
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
