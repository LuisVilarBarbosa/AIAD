import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.TreeMap;

public class MyInterface extends Agent {
    public static final String agentType = "MyInterface";
    public static final String separator = "§";
    private static final String jFrameTitle = "Elevator Management";
    private static final String[] columnNames = {"", "Floor", "Weight", "NumRequests", "State", "NextFloorToStop", "NumPeople", "PeopleEntranceTime", "PeopleExitTime", "MinWaitTime", "MaxWaitTime", "MaxWeight", "MovementTime"};
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
        CommonFunctions.registerOnDFService(this, agentType);
        addBehaviour(new UpdateGUIDimensionsBehaviour());
        addBehaviour(new MyInterfaceBehaviour());
    }

    private void updateGUI() {
        int i = 0;
        for (Map.Entry<AID, String[]> entry : elevatorsData.entrySet()) {
            AID aid = entry.getKey();
            table.setValueAt(aid.getLocalName(), i, 0);
            String[] data = entry.getValue();
            for (int j = 0; j < columnNames.length - 1; j++)
                table.setValueAt(data[j], i, j + 1);
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
