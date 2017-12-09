import jade.core.AID;
import jade.core.Location;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.swing.*;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class MyInterface extends MyAgent {
    public static final String agentType = "MyInterface";
    public static final String separator = "§";
    private static final String jFrameTitle = "Elevator Management";
    private static final String[] columnNames = {"Agent", "Floor", "Weight (kg)", "Num. requests", "State", "Next floor to stop", "Num. people", "CFPs sent", "Proposes sent", "Refuses sent", "Accept proposals sent", "Accept proposals received", "People entrance time (ms)", "People exit time (ms)", "Min. wait time (ms)", "Max. wait time (ms)", "Uptime (ms)", "Downtime (ms)", "Use rate (%)", "Max. weight (kg)", "Movement time (ms)", "Person entrance time (ms)", "Person exit time (ms)", "Has keyboard on request"};
    private static final int preferredWidth = 1200;
    private static final int preferredHeight = 400;
    private final TreeMap<AID, String[]> elevatorsData;
    private final JTable table;
    private final JScrollPane jScrollPane;
    private final String statisticsFilename;

    public MyInterface(final int numElevators, final String statisticsFilename) {
        this.elevatorsData = new TreeMap<>();
        final JFrame jFrame = new JFrame(jFrameTitle);
        final JPanel jPanel = new JPanel();
        this.table = new JTable(new Object[numElevators][columnNames.length], columnNames);
        this.jScrollPane = new JScrollPane(table);
        final Container container = new Container();
        final JTextField jTextField = new JTextField(columnNames.length);
        this.statisticsFilename = statisticsFilename;

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
    public void doMove(Location destination) {
        display("Move not allowed because this agent is connected to a graphical window.");
    }

    private void updateGUI() {
        int i = 0;
        for (final Map.Entry<AID, String[]> entry : elevatorsData.entrySet()) {
            final AID aid = entry.getKey();
            table.setValueAt(aid.getLocalName(), i, 0);
            final String[] data = entry.getValue();
            for (int j = 1; j < columnNames.length; j++)
                table.setValueAt(data[j - 1], i, j);
            i++;
        }
    }

    private void updateFile() {
        final String newline = "\r\n";
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<AID, String[]> entry : elevatorsData.entrySet()) {
            final AID aid = entry.getKey();
            sb.append(aid.getLocalName()).append(":").append(newline);
            final String[] data = entry.getValue();
            for (int j = 1; j < columnNames.length; j++)
                sb.append("\t").append(columnNames[j]).append(": ").append(data[j - 1]).append(newline);
        }

        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(statisticsFilename);
            fileOutputStream.write(sb.toString().getBytes());
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
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
            if (updated) {
                updateGUI();
                updateFile();
            }
        }
    }

    private class UpdateGUIDimensionsBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            jScrollPane.setPreferredSize(jScrollPane.getParent().getSize());
        }
    }
}
