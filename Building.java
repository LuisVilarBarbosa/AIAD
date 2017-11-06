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
	private int numFloors;
	private int numElevators;
	private ArrayList<Integer> maxWeights;
	private Random random = new Random();

	private ArrayList<AID> elevators = new ArrayList();
	private ArrayList<Integer> externalRequests = new ArrayList<>();

	private ArrayList<DFAgentDescription> descriptions = new ArrayList();

	public Building(int numFloors, int numElevators, ArrayList<Integer> maxWeights) {
        if(maxWeights.size() != numElevators)
            throw  new IllegalArgumentException();
        this.numFloors = numFloors;
        this.numElevators = numElevators;
        this.maxWeights = maxWeights;
	}

	protected void setup() {
		DFAgentDescription template = new DFAgentDescription();
		Behaviour b = new SubscriptionInitiator(
				this, DFService.createSubscriptionMessage(this, getDefaultDF(), template, null)) {
			protected void handleInform(ACLMessage inform) {
				try {
					DFAgentDescription[] dfds = DFService.decodeNotification(inform.getContent());
					for(DFAgentDescription d: dfds) {
						descriptions.add(d);
                        System.out.println("Agent " + d.getName().getName() + " created by building.");
					}

				} catch (FIPAException e) {
					e.printStackTrace();
				}

			}
		};
		addBehaviour(b);
		for(int i = 0; i < numElevators; i++)
			try {
		        Elevator elevator = new Elevator(maxWeights.get(i));
				AgentController ac = this.getContainerController().acceptNewAgent("Elevator"+i, elevator);
				ac.start();
				elevators.add(elevator.getAID());
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}

		this.addBehaviour(new BuildingBehaviour());
	}

	private void addRequest(int floor) {
		if (floor < 0 || floor > numFloors)
			throw new IllegalArgumentException("Invalid floor.");
		externalRequests.add(floor);
	}

	private class BuildingBehaviour extends CyclicBehaviour {

		@Override
		public void action() {
			int n = random.nextInt() % (3*numElevators);
			for (int i = 0; i < n; i++) {
                int rand = Math.abs(random.nextInt() % (numFloors * 2));
                if (rand > numFloors)
                    addRequest(0);
                else
                    addRequest(rand);
            }
			for (int externalRequest : externalRequests) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(elevators.get(random.nextInt(elevators.size())));
                msg.setContent(Integer.toString(externalRequest));
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
