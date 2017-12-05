import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.TreeMap;

public class MyInterface extends Agent {
    public static final String agentType = "MyInterface";
    public static final String separator = "§";
    private final String[] columnNames = {"", "Floor", "Weight", "NumRequests", "State", "NextFloorToStop", "NumPeople", "PeopleEntranceTime", "PeopleExitTime", "MinWaitTime", "MaxWaitTime", "MaxWeight", "MovementTime"};
    private final TreeMap<AID, String[]> elevatorsData;
    private final JTable table;

    public MyInterface() {
        this.elevatorsData = new TreeMap<>();
        this.table = new JTable(new Object[3][columnNames.length], columnNames);
        this.table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.table.doLayout();

        JFrame maingui = new JFrame("Gui");
        JButton enter = new JButton("Enter");
        final JTextField movietext = new JTextField(columnNames.length);
        final JScrollPane scrolll = new JScrollPane(table);
        final JLabel titlee = new JLabel("Enter movie name here:");
        final Container cont = new Container();
        JPanel pangui = new JPanel();
        JPanel pangui2 = new JPanel();
        maingui.add(pangui2);
        maingui.add(pangui);
        maingui.setResizable(true);
        maingui.setVisible(true);
        pangui.add(scrolll);
        pangui2.add(cont);
        cont.add(titlee);
        cont.add(movietext);
        cont.add(enter);
        scrolll.getPreferredSize();
        maingui.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        maingui.pack();

        enter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // create building
            }
        });
    }

    @Override
    protected void setup() {
        super.setup();
        CommonFunctions.registerOnDFService(this, agentType);
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
}
