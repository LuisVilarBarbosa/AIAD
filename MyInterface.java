import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.TreeMap;

public class MyInterface extends MyAgent {
    public static final String agentType = "MyInterface";
    public static final String separator = "§";
    private static final String jFrameTitle = "Elevator Management";
    private static final String[] columnNames = {"", "Floor", "Weight", "Num. requests", "State", "Next floor to stop", "Num. people", "CFPs sent", "Proposes sent", "Refuses sent", "Accepted proposals sent", "Accepted proposals received", "People entrance time", "People exit time", "Min. wait time", "Max. wait time", "Uptime", "Downtime", "Use rate", "Max. weight", "Movement time", "Person entrance time", "Person exit time", "Has keyboard on request"};
    private static final int preferredWidth = 1200;
    private static final int preferredHeight = 400;
    private final TreeMap<AID, String[]> elevatorsData;
    private final JTable table;
    private final JScrollPane jScrollPane;

    public MyInterface(final int numElevators) {
        this.elevatorsData = new TreeMap<>();
        final JFrame jFrame = new JFrame(jFrameTitle);
        final JPanel jPanel = new JPanel();
        this.table = new JTable(new Object[numElevators][columnNames.length], columnNames);
        this.jScrollPane = new JScrollPane(table);
        final Container container = new Container();
        final JTextField jTextField = new JTextField(columnNames.length);

        jFrame.add(jPanel);
        jFrame.setResizable(true);
        jFrame.setVisible(true);
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jPanel.add(this.jScrollPane);
        this.jScrollPane.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        this.table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        jPanel.add(container);
        container.add(jTextField);

        jFrame.pack();
    }

    @Override
    protected void setup() {
        super.setup();
        registerOnDFService(agentType);
        addBehaviour(new UpdateGUIDimensionsBehaviour());
        addBehaviour(new MyInterfaceBehaviour());
    }

    @Override
    protected void takeDown() {
        super.takeDown();
        deregisterOnDFService();
    }

    private void updateGUI() {
        int i = 0;
        for (Map.Entry<AID, String[]> entry : elevatorsData.entrySet()) {
            AID aid = entry.getKey();
            table.setValueAt(aid.getLocalName(), i, 0);
            String[] data = entry.getValue();
            for (int j = 1; j < columnNames.length; j++)
                table.setValueAt(data[j - 1], i, j);
            i++;
        }
    }

    private class MyInterfaceBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage msg;
            boolean updated = false;
            while ((msg = receive(MessageTemplate.MatchProtocol(MyInterface.agentType))) != null) {
                updated = true;
                elevatorsData.put(msg.getSender(), msg.getContent().split(MyInterface.separator));
            }
            if (updated)
                updateGUI();
        }
    }

    private class UpdateGUIDimensionsBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            jScrollPane.setPreferredSize(jScrollPane.getParent().getSize());
        }
    }
}
