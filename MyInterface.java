import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.management.timer.Timer;
import java.io.IOException;
import java.util.TreeMap;

public class MyInterface extends Agent {
    public static final String agentType = "MyInterface";
    private final TreeMap<AID, String> elevatorMessages;

    public MyInterface() {
        this.elevatorMessages = new TreeMap<>();
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
        for (String elevatorMessage : elevatorMessages.values())
            sb.append(elevatorMessage);
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
                while ((msg = receive(MessageTemplate.MatchProtocol(MyInterface.agentType))) != null)
                    elevatorMessages.put(msg.getSender(), msg.getContent());
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
