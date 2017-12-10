import jade.Boot;
import jade.core.Profile;
import jade.core.ProfileException;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.leap.Properties;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

// Some info about JADE agents in https://pt.slideshare.net/AryanRathore4/all-about-agents-jade

// Based on the Boot class source code of the version 4.5.0
public class MyBoot extends Boot {
    public static final int exitCodeOnError = -1;
    private static final String DEFAULT_FILENAME = "default.properties";
    private static final String NUM_FLOORS_PARAMETER = "numFloors";
    private static final String NUM_ELEVATORS_PARAMETER = "numElevators";
    private static final String MAX_WEIGHT_PARAMETER = "maxWeight";
    private static final String MOVEMENT_TIME_PARAMETER = "movementTime";
    private static final String REQUEST_GENERATION_INTERVAL = "requestGenerationInterval";
    private static final String NUM_REQUESTS_PER_INTERVAL = "numRequestsPerInterval";
    private static final String PERSON_ENTRANCE_TIME = "personEntranceTime";
    private static final String PERSON_EXIT_TIME = "personExitTime";
    private static final String KEYBOARD_FOR_REQUEST = "keyboardForRequest";
    private static final String STATISTICS_FILENAME = "statisticsFilename";
    private static final String ELEVATORS_NEGOTIATION_ALLOWED = "elevatorsNegotiationAllowed";
    private static final String DEFAULT_NUM_FLOORS = "3";
    private static final String DEFAULT_NUM_ELEVATORS = "5";
    private static final String DEFAULT_MAX_WEIGHT = "500";
    private static final String DEFAULT_MOVEMENT_TIME = "1000";
    private static final String DEFAULT_REQUEST_GENERATION_INTERVAL = "10000";
    private static final String DEFAULT_NUM_REQUESTS_PER_INTERVAL = "rand";
    private static final String DEFAULT_PERSON_ENTRANCE_TIME = "1000";
    private static final String DEFAULT_PERSON_EXIT_TIME = "1000";
    private static final String DEFAULT_KEYBOARD_FOR_REQUEST = "false";
    private static final String DEFAULT_STATISTICS_FILENAME = "ElevatorsStatistics.txt";
    private static final String DEFAULT_ELEVATORS_NEGOTIATION_ALLOWED = "true";

    /**
     * Fires up the <b><em>JADE</em></b> system.
     * This method initializes the Profile Manager and then starts the
     * bootstrap process for the <B><em>JADE</em></b>
     * agent platform.
     */
    public static void main(String[] args) {
        try {
            // Create the Profile
            final ProfileImpl p;
            final ContainerController containerController;
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

            final int numFloors = Integer.parseInt(p.getParameter(NUM_FLOORS_PARAMETER, DEFAULT_NUM_FLOORS));
            final int numElevators = Integer.parseInt(p.getParameter(NUM_ELEVATORS_PARAMETER, DEFAULT_NUM_ELEVATORS));
            final long reqGenInterval = Integer.parseInt(p.getParameter(REQUEST_GENERATION_INTERVAL, DEFAULT_REQUEST_GENERATION_INTERVAL));
            final String numRequestsPerIntervalStr = p.getParameter(NUM_REQUESTS_PER_INTERVAL, DEFAULT_NUM_REQUESTS_PER_INTERVAL);
            final String statisticsFilename = p.getParameter(STATISTICS_FILENAME, DEFAULT_STATISTICS_FILENAME);
            final String keyboardForReqStr = p.getParameter(KEYBOARD_FOR_REQUEST, DEFAULT_KEYBOARD_FOR_REQUEST);
            final String elevatorsNegotiationAllowedStr = p.getParameter(ELEVATORS_NEGOTIATION_ALLOWED, DEFAULT_ELEVATORS_NEGOTIATION_ALLOWED);
            final boolean keyboardForReq = parseStringToBoolean(keyboardForReqStr, KEYBOARD_FOR_REQUEST);
            final boolean elevatorsNegotiationAllowed = parseStringToBoolean(elevatorsNegotiationAllowedStr, ELEVATORS_NEGOTIATION_ALLOWED);
            verifyFile(statisticsFilename);

            final ArrayList<ElevatorProperties> elevatorsProperties = new ArrayList<>();
            for (int i = 0; i < numElevators; i++) {
                final int maxWeight = Integer.parseInt(p.getParameter(MAX_WEIGHT_PARAMETER + i, DEFAULT_MAX_WEIGHT));
                final long movementTime = Long.parseLong(p.getParameter(MOVEMENT_TIME_PARAMETER + i, DEFAULT_MOVEMENT_TIME));
                final int entranceTime = Integer.parseInt(p.getParameter(PERSON_ENTRANCE_TIME + i, DEFAULT_PERSON_ENTRANCE_TIME));
                final int exitTime = Integer.parseInt(p.getParameter(PERSON_EXIT_TIME + i, DEFAULT_PERSON_EXIT_TIME));
                ElevatorProperties elevatorProperties = new ElevatorProperties(maxWeight, movementTime, entranceTime, exitTime);
                elevatorsProperties.add(elevatorProperties);
            }

            BuildingProperties buildingProperties = new BuildingProperties(numFloors, numElevators, keyboardForReq, elevatorsNegotiationAllowed);
            Building building;
            if (numRequestsPerIntervalStr.equalsIgnoreCase("rand"))
                building = new Building(buildingProperties, reqGenInterval, elevatorsProperties);
            else
                building = new Building(buildingProperties, reqGenInterval, Integer.parseInt(numRequestsPerIntervalStr), elevatorsProperties);

            final AgentController buildingAC = containerController.acceptNewAgent(Building.agentType, building);
            buildingAC.start();
            final AgentController myInterfaceAC = containerController.acceptNewAgent(MyInterface.agentType, new MyInterface(numElevators, statisticsFilename));
            myInterfaceAC.start();
        } catch (ProfileException pe) {
            Console.displayError("Error creating the Profile [" + pe.getMessage() + "]");
            pe.printStackTrace();
            printUsage();
            System.exit(MyBoot.exitCodeOnError);
        } catch (IllegalArgumentException iae) {
            Console.displayError("Command line arguments format error. " + iae.getMessage());
            iae.printStackTrace();
            printUsage();
            System.exit(MyBoot.exitCodeOnError);
        } catch (StaleProxyException e) {
            e.printStackTrace();
            System.exit(MyBoot.exitCodeOnError);
        }
    }

    public static void printUsage() {
        String usage = "\nUsage:\n" +
                "Command-type 1: java -classpath \"jade.jar;.\" MyBoot [<filename>]\n" +
                "Command-type 2: java -classpath \"jade.jar;.\" MyBoot [-gui] -" + NUM_FLOORS_PARAMETER + " 5 -" + NUM_ELEVATORS_PARAMETER + " 3 -" + MAX_WEIGHT_PARAMETER + "0 100 -" + MAX_WEIGHT_PARAMETER + "1 200 -" + MAX_WEIGHT_PARAMETER + "2 300 -" + MOVEMENT_TIME_PARAMETER + "0 1000 -" + MOVEMENT_TIME_PARAMETER + "1 500 -" + MOVEMENT_TIME_PARAMETER + "2 2000 -" + PERSON_ENTRANCE_TIME + "0 1000 -" + PERSON_ENTRANCE_TIME + "1 1000 -" + PERSON_ENTRANCE_TIME + "2 1000 -" + PERSON_EXIT_TIME + "0 1000 -" + PERSON_EXIT_TIME + "1 1000 -" + PERSON_EXIT_TIME + "2 1000 -" + KEYBOARD_FOR_REQUEST + " false -" + STATISTICS_FILENAME + " " + DEFAULT_STATISTICS_FILENAME + " -" + ELEVATORS_NEGOTIATION_ALLOWED + " " + DEFAULT_ELEVATORS_NEGOTIATION_ALLOWED + "\n\n" +
                "For command-type 1, if no filename is indicated, it will use the default file with name '" + DEFAULT_FILENAME + "'.\n" +
                "For command-type 2, there should be as many '" + MAX_WEIGHT_PARAMETER + "X', '" + MOVEMENT_TIME_PARAMETER + "X', '" + PERSON_ENTRANCE_TIME + "X' and '" + PERSON_EXIT_TIME + "X' as elevators.\n" +
                "For both commands types, if some of the values are not indicated, the corresponding default will be associated to the missing value:\n" +
                "  " + NUM_FLOORS_PARAMETER + "=" + DEFAULT_NUM_FLOORS + "\n" +
                "  " + NUM_ELEVATORS_PARAMETER + "=" + DEFAULT_NUM_ELEVATORS + "\n" +
                "  " + MAX_WEIGHT_PARAMETER + "=" + DEFAULT_MAX_WEIGHT + "\n" +
                "  " + MOVEMENT_TIME_PARAMETER + "=" + DEFAULT_MOVEMENT_TIME + "\n" +
                "  " + REQUEST_GENERATION_INTERVAL + "=" + DEFAULT_REQUEST_GENERATION_INTERVAL + "\n" +
                "  " + NUM_REQUESTS_PER_INTERVAL + "=" + DEFAULT_NUM_REQUESTS_PER_INTERVAL + "\n" +
                "  " + PERSON_ENTRANCE_TIME + "=" + DEFAULT_PERSON_ENTRANCE_TIME + "\n" +
                "  " + PERSON_EXIT_TIME + "=" + DEFAULT_PERSON_EXIT_TIME + "\n" +
                "  " + KEYBOARD_FOR_REQUEST + "=" + DEFAULT_KEYBOARD_FOR_REQUEST + "\n" +
                "  " + STATISTICS_FILENAME + "=" + DEFAULT_STATISTICS_FILENAME + "\n" +
                "  " + ELEVATORS_NEGOTIATION_ALLOWED + "=" + DEFAULT_ELEVATORS_NEGOTIATION_ALLOWED + "\n\n" +
                "Additional options (MyBoot extends Boot):\n";
        Console.display(usage);
        Boot.printUsage();
    }

    private static boolean parseStringToBoolean(final String varValue, final String varName) {
        final boolean bool;
        if (varValue.equalsIgnoreCase("true"))
            bool = true;
        else if (varValue.equalsIgnoreCase("false"))
            bool = false;
        else
            throw new IllegalArgumentException("Invalid '" + varName + "' mode: " + varValue);
        return bool;
    }

    private static void verifyFile(final String filename) {
        final File file = new File(filename);
        if (!file.exists()) {
            try {
                if (!file.createNewFile())
                    Console.displayError("Unable to create the file '" + filename + "'");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!file.isFile() || !file.canWrite())
            throw new IllegalArgumentException("'" + filename + "' is not a file or isn't writable.");
    }
}
