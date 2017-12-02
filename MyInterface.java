import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.management.timer.Timer;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class MyInterface extends Agent {
    public static final String agentType = "MyInterface";
    private final TreeMap<String, ElevatorState> elevatorStates;

    public MyInterface() {
        this.elevatorStates = new TreeMap<>();
    }

    @Override
    protected void setup() {
        super.setup();
        addBehaviour(new MyInterfaceBehaviour());
    }

    private void display(final String str) {
        clearConsole();
        System.out.println(str);
    }

    private String designScreen() {
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ElevatorState> entry : elevatorStates.entrySet()) {
            final String name = entry.getKey();
            final ElevatorState es = entry.getValue();
            sb.append(name).append(":\n");
            sb.append("\tFloor=").append(es.getActualFloor());
            sb.append(" Weight=").append(es.getActualWeight());
            sb.append(" NumRequests=").append(es.getInternalRequestsSize());
            sb.append(" State=").append(es.getState()).append("\n");
            sb.append("\tInitialFloor=").append(es.getInitialFloor());
            sb.append(" DestinationFloor=").append(es.getDestinationFloor()).append("\n");
            sb.append("\tMaxWeight=").append(es.getMaxWeight());
            sb.append(" MovementTime=").append(es.getMovementTime()).append("\n");
            for (final String info : es.getInformation())
                sb.append("\t").append(info).append("\n");
            sb.append("\n");
        }
        return sb.toString();
    }

    private static void clearConsole() {
        try {
            final String os = System.getProperty("os.name");
            if (os.contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                Runtime.getRuntime().exec("clear");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class MyInterfaceBehaviour extends SimpleBehaviour {

        @Override
        public void action() {
            while (true) {
                ACLMessage msg;
                while ((msg = receive(MessageTemplate.MatchProtocol(MyInterface.agentType))) != null) {
                    final ElevatorState elevatorState = new ElevatorState(msg.getContent());
                    elevatorStates.put(msg.getSender().getLocalName(), elevatorState);
                }
                display(designScreen());
                CommonFunctions.sleep(Timer.ONE_SECOND);
            }
        }

        @Override
        public boolean done() {
            return false;
        }
    }
}
