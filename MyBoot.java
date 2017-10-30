import jade.Boot;
import jade.core.Profile;
import jade.core.ProfileException;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.Logger;
import jade.util.leap.Properties;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;

// Based on the Boot class source code of the version 4.5.0
public class MyBoot extends Boot {
    public static final String DEFAULT_FILENAME = "default.properties";
    private static Logger logger = Logger.getMyLogger("jade.MyBoot");

    /**
     * Fires up the <b><em>JADE</em></b> system.
     * This method initializes the Profile Manager and then starts the
     * bootstrap process for the <B><em>JADE</em></b>
     * agent platform.
     */
    public static void main(String[] args) {
        try {
            // Create the Profile
            ProfileImpl p = null;
            ContainerController containerController;
            if (args.length > 0) {
                if (args[0].startsWith("-")) {
                    // Settings specified as command line arguments
                    Properties pp = parseCmdLineArgs(args);
                    if (pp != null) {
                        p = new ProfileImpl(pp);
                    } else {
                        // One of the "exit-immediately" options was specified!
                        return;
                    }
                } else {
                    // Settings specified in a property file
                    p = new ProfileImpl(args[0]);
                }
            } else {
                // Settings specified in the default property file
                p = new ProfileImpl(DEFAULT_FILENAME);
            }

            // Start a new JADE runtime system
            Runtime.instance().setCloseVM(true);
            //#PJAVA_EXCLUDE_BEGIN
            // Check whether this is the Main Container or a peripheral container
            if (p.getBooleanProperty(Profile.MAIN, true)) {
                containerController = Runtime.instance().createMainContainer(p);
            } else {
                containerController = Runtime.instance().createAgentContainer(p);
            }
            //#PJAVA_EXCLUDE_END
            /*#PJAVA_INCLUDE_BEGIN
            // Starts the container in SINGLE_MODE (Only one per JVM)
			Runtime.instance().startUp(p);
			#PJAVA_INCLUDE_END*/

            try {
                // update "USAGE" message
                int numFloors = Integer.parseInt(p.getParameter("numFloors", "3"));
                int numElevators = Integer.parseInt(p.getParameter("numElevators", "5"));
                ArrayList<Integer> maxWeights = new ArrayList<>();
                for(int i = 1; i <= numElevators; i++)
                    maxWeights.add(Integer.parseInt(p.getParameter("maxWeight" + i, "500")));

                AgentController ac = containerController.acceptNewAgent("Building", new Building(numFloors, numElevators, maxWeights));
                ac.start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        } catch (ProfileException pe) {
            System.err.println("Error creating the Profile [" + pe.getMessage() + "]");
            pe.printStackTrace();
            printUsage();
            //System.err.println("Usage: java jade.MyBoot <filename>");
            System.exit(-1);
        } catch (IllegalArgumentException iae) {
            System.err.println("Command line arguments format error. " + iae.getMessage());
            iae.printStackTrace();
            printUsage();
            //System.err.println("Usage: java jade.MyBoot <filename>");
            System.exit(-1);
        }
    }
}
