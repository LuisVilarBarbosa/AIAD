import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
import java.util.*;

public class MyInterface extends Agent {
    public static final String agentType = "MyInterface";
    private int backtrackCounter;
    private TreeMap<String, ElevatorState> elevatorStates;

    public MyInterface() {
        this.backtrackCounter = 0;
        this.elevatorStates = new TreeMap<>();
    }

    @Override
    protected void setup() {
        super.setup();
        addBehaviour(new MyInterfaceBehaviour());
    }

    private void displayProgress(String str) {
        displayAux(str);
        backtrackCounter = str.length();
    }

    private void display(String str) {
        displayAux(str);
        backtrackCounter = 0;
    }

    private void displayAux(String str) {
        /*StringBuilder sb = new StringBuilder();
        for (int i = 0; i < backtrackCounter; i++)
            sb.append('\b');
        sb.append(str);
        System.out.print(sb.toString());*/
        clearConsole();
        System.out.println(str);
    }

    private String designScreen() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String,ElevatorState> entry : elevatorStates.entrySet()) {
            String name = entry.getKey();
            ElevatorState es = entry.getValue();
            sb.append(name).append(":");
            sb.append(" Floor=").append(es.getActualFloor());
            sb.append(" Weight=").append(es.getActualWeight());
            sb.append(" NumRequests=").append(es.getInternalRequestsSize());
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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class MyInterfaceBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage msg;
            while ((msg = receive(MessageTemplate.MatchProtocol(MyInterface.agentType))) != null) {
                ElevatorState elevatorState = new ElevatorState(msg.getContent());
                elevatorStates.put(msg.getSender().getLocalName(), elevatorState);
                displayProgress(designScreen());
            }
        }
    }
}
