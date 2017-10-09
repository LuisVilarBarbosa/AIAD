import java.util.ArrayList;
import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
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

	public void setup() {
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
					msg.addReceiver(elevators.get(j));
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
