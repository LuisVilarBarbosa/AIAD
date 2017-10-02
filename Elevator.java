import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;

import java.util.ArrayList;
import java.util.Random;

public class Elevator extends Agent {
    // floor 0 must exist
    private static int minFloor = -3;
    private static int maxFloor = 20;
    private static int nElevators = 3;  // not in use
    private int maxPeople = 8;  // not in use
    private int moveTime = 20;
    private int actualFloor = 0;
    private ArrayList<Integer> requests = new ArrayList<>();
    private Random random = new Random();

    public void setup() {
        this.addBehaviour(new ElevatorBehaviour());
    }

    public void addRequest(int floor) {
        if (floor < minFloor || floor > maxFloor)
            throw new IllegalArgumentException("Invalid floor.");
        requests.add(floor);
    }

    private class ElevatorBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            if (!requests.isEmpty()) {
                int nextFloor = requests.remove(0);
                System.out.println("Floor: " + nextFloor);
                try {
                    Thread.sleep(moveTime * Math.abs(nextFloor - actualFloor));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                actualFloor = nextFloor;
            }

            int n = random.nextInt() % 50;
            for (int i = 0; i < n; i++) {
                int rand = Math.abs(random.nextInt() % ((maxFloor - minFloor) * 2)) - minFloor;
                if (rand > maxFloor)
                    addRequest(0);
                else
                    addRequest(rand);
            }
        }
    }
}
