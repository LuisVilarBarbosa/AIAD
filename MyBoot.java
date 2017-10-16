import jade.Boot;
import jade.core.Profile;
import jade.core.ProfileException;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.Logger;
import jade.util.leap.Properties;

// Based on the Boot class source code of the version 4.5.0
public class MyBoot extends Boot {
    public static final String DEFAULT_FILENAME = "leap.properties";
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
                throw new ProfileException("No profile has been specified.");
                // Settings specified in the default property file
                //p = new ProfileImpl(DEFAULT_FILENAME);
            }

            // Start a new JADE runtime system
            Runtime.instance().setCloseVM(true);
            //#PJAVA_EXCLUDE_BEGIN
            // Check whether this is the Main Container or a peripheral container
            if (p.getBooleanProperty(Profile.MAIN, true)) {
                Runtime.instance().createMainContainer(p);
            } else {
                Runtime.instance().createAgentContainer(p);
            }
            //#PJAVA_EXCLUDE_END
            /*#PJAVA_INCLUDE_BEGIN
            // Starts the container in SINGLE_MODE (Only one per JVM)
			Runtime.instance().startUp(p);
			#PJAVA_INCLUDE_END*/
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
