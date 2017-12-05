import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
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
        CommonFunctions.registerOnDFService(this, agentType);
        addBehaviour(new MyInterfaceBehaviour());
    }

    private void display(final String str) {
        clearConsole();
        MyBoot.logger.info(str);
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
            MyBoot.logger.warning(e.toString());
        }
    }

    private class MyInterfaceBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage msg;
            boolean updated = false;
            while ((msg = receive(MessageTemplate.MatchProtocol(MyInterface.agentType))) != null) {
                elevatorMessages.put(msg.getSender(), msg.getContent());
                updated = true;
            }
            if (updated)
                display(designScreen());
            CommonFunctions.sleep(Timer.ONE_SECOND);
        }
    }
}
