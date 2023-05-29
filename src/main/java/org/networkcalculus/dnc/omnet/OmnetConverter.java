package org.networkcalculus.dnc.omnet;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import org.networkcalculus.dnc.curves.ArrivalCurve;
import org.networkcalculus.dnc.curves.ServiceCurve;
import org.networkcalculus.dnc.network.server_graph.Flow;
import org.networkcalculus.dnc.network.server_graph.Server;
import org.networkcalculus.dnc.network.server_graph.ServerGraph;
import org.networkcalculus.dnc.network.server_graph.Turn;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

class OmnetConnection {
    private String src;
    private String dest;

    OmnetConnection(String src, String dest) {
        this.src = src;
        this.dest = dest;
    }

    public String getDst() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }
}

class OmnetDevice {
    private final String identifier;
    private final SubmoduleTypes type;

    private final double servicerateLimit;

    OmnetDevice(String identifier, SubmoduleTypes type, double serviceRateLimitBps) {
        this.identifier = identifier;
        this.type = type;
        this.servicerateLimit = serviceRateLimitBps;
    }

    public SubmoduleTypes getType() {
        return type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public double getServicerateLimit() {
        return servicerateLimit;
    }
}

enum SubmoduleTypes {
    TSN_DEVICE("TSNFlowMonitorDevice"),
    TSN_SWITCH("TsnSwitch")
    ;

    private final String str;

    SubmoduleTypes(final String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}

class OmnetUDPSink {
    private final String name;
    private final int port;

    private final String flowid;
    private final boolean enableMeasurements;

    OmnetUDPSink(String name, String flowid, int port, boolean withMeasurement) {
        this.name = name;
        this.flowid = flowid;
        this.port = port;
        this.enableMeasurements = withMeasurement;
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }

    public String getType() {
        return "UdpSinkApp";
    }

    public String getFlowid() {
        return flowid;
    }

    public boolean getWithMeasurement() {
        return enableMeasurements;
    }
}

class OmnetSourceDestination {
    private final String address;
    private final int port;

    OmnetSourceDestination(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getAddress() {
        return address;
    }
}

class OmnetSource {
    // The packet production in us
    private final double pInterval;
    // The packet length in bytes
    private final long burstLength;
    private final String name;
    private final OmnetSourceDestination dest;

    private final String flowid;

    private final boolean enableMeasurements;

    OmnetSource(String name, OmnetSourceDestination dest, String flowid, long burstLength, double pIntervalUs, boolean withMeasurement) {
        this.name = name;
        this.dest = dest;
        this.flowid = flowid;
        this.burstLength = burstLength;
        this.pInterval = pIntervalUs;
        this.enableMeasurements = withMeasurement;
    }

    public String getName() {
        return name;
    }

    public OmnetSourceDestination getDst() {
        return dest;
    }

    public String getType() {
        return "UdpSourceApp";
    }

    public double getInterval() {
        return pInterval;
    }

    public long getPacketLength() {
        return OmnetConverter.MAX_UDP_MSG_SIZE;
    }

    public long getBurstLength() {
        return burstLength;
    }

    public String getFlowid() {
        return flowid;
    }

    public boolean getWithMeasurement() {
        return enableMeasurements;
    }
}


/**
 * Usage of this converter requires the following pre-conditions:
 * (0) A system with a working "make" setup installed. (If you built omnet from source this is already taken care of)
 * (1) A installation of Omnet in the system path, e.g you should be able to run opp_makemake without specifying a path
 * (2) A valid path to the compiled version of the inet framework
 */
public class OmnetConverter {
    public static final long MAX_MTU_BYTES = 1500;
    public static long MAX_UDP_MSG_SIZE = accountForUDPOverhead(MAX_MTU_BYTES);

    // Default settings
    private static final long DEFAULT_SIMULATION_TIME_LIMIT = 5;
    // Constants
    public static final String SIMULATION_BINARY_NAME = "simulation";
    public static final String SIMULATION_TEMP_FOLDER = "temp";
    public static final String SIMULATION_NAME_PREFIX = "omnet-";
    public static final String SCALAR_RESULT_FILE = "data.sca";



    /**
     * The JinJava templating engine instance
     */
    Jinjava jinJava;

    // Path to the inet installation, required for compiling and executing the simulation
    Path pathToInet;

    // Show the simulation output by default
    boolean showSimulationOutput = true;

    /**
     * Retrieves the identifier for a server.
     *
     * @param srv The server object for which to retrieve the identifier.
     * @return The identifier string for the server.
     */
    public static String getServerIdentifier(Server srv) {
        return "Server" + srv.getId();
    }

    /**
     * Retrieves the identifier for a flow, indicating whether it is the start or end of the flow.
     *
     * @param flow The flow object for which to retrieve the identifier.
     * @param isStart Determines whether the identifier is for the start or end of the flow.
     * @return The identifier string for the flow.
     */
    public static String getFlowIdentifier(Flow flow, boolean isStart) {
        return "Flow" + flow.getId() + (isStart ? "Start" : "End");
    }

    /**
     * Converts an input stream to a formatted string.
     *
     * @param stream The input stream to be converted.
     * @return A string representation of the input stream's contents.
     */
    public static String getPrettyStringFromInputStream(InputStream stream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        return reader.lines().collect(Collectors.joining("\n"));
    }

    /**
     * Creates a new instance of the OmnetConverter
     * @param inetPath the path to a **compiled** version of the inet framework.
     */
    public OmnetConverter(String inetPath) {
        JinjavaConfig conf = JinjavaConfig.newBuilder()
        .withLstripBlocks(true)
        .withTrimBlocks(true)
        .build();

        this.jinJava = new Jinjava(conf);
        this.pathToInet = Paths.get(inetPath);
    }

    /**
     * Creates a temporary folder for the simulation using the specified UUID.
     *
     * @param uuid The UUID to be used for creating the temporary folder.
     * @return The created temporary folder as a {@link File} object, or null if an error occurred.
     */
    public static File createTempFolder(String uuid) {
        try {
            Path folderPath = Paths.get(SIMULATION_TEMP_FOLDER, SIMULATION_NAME_PREFIX + uuid);
            // Create a new folder
            Files.createDirectories(folderPath);

            File folder = folderPath.toFile();
            // re-use existing folders
            if (folder.exists() && folder.isDirectory()) {
                return folder;
            }
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }

        return null;
    }

    /**
     * Compiles the simulation by generating makefiles and compiling the executable.
     *
     * @param workingDirectory The working directory where the simulation should be compiled.
     * @return True if the simulation was successfully compiled, false otherwise.
     */
    public boolean compileSimulation(File workingDirectory) {
        // First run the makefile generation
        if (!runCommand(new String[] {
                "opp_makemake",
                "-f", "--deep",
                // Define the inet makefile variable that is used in a later step
                String.format("-KINET_PROJ=%s", pathToInet),
                // Only link against the cmd version, keeps the dependencies small
                "-u", "Cmdenv",
                // Use a generic name
                "-o", "simulation",
                // Define the inet import
                "-DINET_IMPORT",
                "-L$(INET_PROJ)/src",
                "-lINET$(D)"
        }, workingDirectory, false)) {
            System.err.println("creating the make files for omnet failed");
            return false;
        }

        // Now compile the executable
        // todo: this is not cross-platform compatible
        if (!runCommand(new String[]{"make"}, workingDirectory, false)) {
            System.err.println("compiling the simulation executable failed");
            return false;
        }

        return true;
    }


    /**
     * Runs the simulation with the specified working directory and INET path.
     *
     * @param workingDirectory The working directory where the simulation should be executed.
     * @return True if the simulation ran successfully, false otherwise.
     */
    public boolean runSimulation(File workingDirectory) {
        return runCommand(new String[]{
                "./" + SIMULATION_BINARY_NAME,
                "-m", "-u", "Cmdenv",
                // Define the inet src folder, so it finds the required NEDs
                "-n", ".:" + pathToInet.resolve("src"),
                "omnetpp.ini"
        }, workingDirectory, showSimulationOutput);
    }


    /**
     * Executes a command with the specified arguments in the specified working directory.
     *
     * @param cmd The command and its arguments to be executed.
     * @param workingDirectory The working directory where the command should be executed.
     * @param withOutput Determines whether the command output should be displayed in the console.
     * @return True if the command executed successfully, false otherwise.
     */
    private boolean runCommand(String[] cmd, File workingDirectory, boolean withOutput) {
        // Create a new process builder and set the command
        ProcessBuilder builder = new ProcessBuilder(cmd);

        // Set the working directory
        builder.directory(workingDirectory);

        // If live output is desired, redirecting it to the standard IO is the easiest solution
        if (withOutput) {
            builder.inheritIO();
        }

        // Start the process
        try {
            Process process = builder.start();

            // Output what we are about to do
            System.out.println("Running command: " + Arrays.toString(cmd));

            // todo: set up threaded streams to read stdout and redirect it to the console
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Running a command failed \n"
                        + getPrettyStringFromInputStream(process.getInputStream())
                        + "\n STDERR:" + getPrettyStringFromInputStream(process.getErrorStream())
                );

                return false;
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    // todo wip: this is a prototype of the conversion function, code will be split into methods later on
    public void convert(final ServerGraph sg, final Flow flowOfInterest) throws Exception {
        // The lists we make available in jinja2 later
        List<OmnetDevice> devices = new LinkedList<>();
        List<OmnetConnection> connections = new LinkedList<>();

        Map<String, List<OmnetUDPSink>> sinks = new HashMap<>();
        Map<String, List<OmnetSource>> sources = new HashMap<>();

        for (final Server srv : sg.getServers() ) {
            // We register them as a switch for now because they transparently route packets
            if (srv.useMaxSC()) {
                throw new RuntimeException("no idea how to support max service curves for now.");
            }

            // Obtain the service curve limit
            ServiceCurve curve = srv.getServiceCurve();
            int serviceRateLimitBps = 0;
            if (curve != null) {
                serviceRateLimitBps = (int) (curve.getUltAffineRate().doubleValue() + 0.5);
            }

            devices.add(new OmnetDevice(getServerIdentifier(srv), SubmoduleTypes.TSN_SWITCH, serviceRateLimitBps));
        }

        // todo we can get the physical "turns" but how do we do paths?
        // fixme: routing is not implemented
        for (Turn turn : sg.getTurns()) {
            connections.add(
                    // Create a new omnet connection object
                    new OmnetConnection(
                            getServerIdentifier(turn.getSource()),
                            getServerIdentifier(turn.getDest())
                    )
            );
        }

        // Grab the flows and add them as TSN_DEVICE
        for (Flow flow : sg.getFlows()) {
            // Check if the flow is our flow of interest
            boolean isFoi = flow.equals(flowOfInterest);
            
            LinkedList<Server> servers = flow.getServersOnPath();

            // The flow source
            String startFlowID = getFlowIdentifier(flow, true);
            Server startServer = servers.getFirst();
            devices.add(new OmnetDevice(startFlowID, SubmoduleTypes.TSN_DEVICE,0));
            connections.add(new OmnetConnection(
                    startFlowID,
                    getServerIdentifier(startServer)
            ));

            // The flow target
            String endFlowID = getFlowIdentifier(flow, false);
            Server endServer = servers.getLast();
            devices.add(new OmnetDevice(endFlowID, SubmoduleTypes.TSN_DEVICE, 0));
            connections.add(new OmnetConnection(
                    endFlowID,
                    getServerIdentifier(endServer)
            ));


            String monitorFlowID = "f" + flow.getId();
            // Create the src and sink
            String srcID = monitorFlowID+"Source";
            String sinkID = monitorFlowID+"Sink";

            ArrivalCurve arrivalCurve = flow.getArrivalCurve();

            int currentSinkPort = 1000;
            OmnetSource src = new OmnetSource(
                    srcID,
                    new OmnetSourceDestination(endFlowID, currentSinkPort),
                    monitorFlowID,

                    // Get the burst value (we assume bit), convert to byte and substract the "overhead"
                    getUDPBurstValueFromRaw(arrivalCurve.getBurst().doubleValue()),

                    // Grab the rate in bit/s from the arrival curve and round up
                    calculateProductionInterval(
                            MAX_UDP_MSG_SIZE,
                            (int) (arrivalCurve.getUltAffineRate().doubleValue() + 0.5)
                    ),
                    isFoi
            );

            List<OmnetSource> sourceList = sources.getOrDefault(startFlowID,  new LinkedList<>());
            sourceList.add(src);
            sources.put(startFlowID, sourceList);

            // Create a sink object that fits with the source
            OmnetUDPSink sink = new OmnetUDPSink(sinkID, monitorFlowID, currentSinkPort, isFoi);
            List<OmnetUDPSink> appList = sinks.getOrDefault(endFlowID,  new LinkedList<>());
            appList.add(sink);
            sinks.put(endFlowID, appList);
        }

        java.net.URL nedTemplate = getClass().getResource("tsn_netgraph.jinja");
        if (nedTemplate == null) {
            throw new FileNotFoundException("omnet tsn netgraph template not found");
        }

        java.net.URL iniTemplate = getClass().getResource("tsn_omnetpp.jinja");
        if (iniTemplate == null) {
            throw new FileNotFoundException("omnetpp ini template not found");
        }


        // Templating output starts here
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("devices", devices);
        ctx.put("connections", connections);
        ctx.put("sinks", sinks);
        ctx.put("sources", sources);

        // todo: allow changing the max simulation time in seconds
        ctx.put("max_time_s", DEFAULT_SIMULATION_TIME_LIMIT);

        // Create a temporary todo: (permanent heh) output folder
        String uniqueID = String.valueOf(sg.hashCode());
        File temporaryFolder = createTempFolder(uniqueID);
        if (temporaryFolder == null) {
            throw new FileNotFoundException("temporary folder could not be created");
        }

        try {
            // Write the network graph ned file
            writeToFile(
                Paths.get(temporaryFolder.getPath(), "package.ned"), 
                jinJava.render(Files.readString(Paths.get(nedTemplate.toURI())), ctx)
            );

            // Write the omnetpp.ini file
            writeToFile(
                Paths.get(temporaryFolder.getPath(), "omnetpp.ini"),
                jinJava.render(Files.readString(Paths.get(iniTemplate.toURI())), ctx)
            );
        } catch (IOException ex) {

        }


        // Compile the simulation files
        // todo: move this to its own function so we have a better api
        compileSimulation(temporaryFolder);
        runSimulation(temporaryFolder);

        double e2e = ScaExtractor.getSimulationEndToEndDelay(Paths.get(temporaryFolder.getPath(), SCALAR_RESULT_FILE));
        System.out.println("Simulation completed, e2e: " + e2e);

        // todo: Save csv sc, ac, delay params
    }

    /**
     * Writes the provided data to a file at the specified output path.
     *
     * @param outputPath The path where the file should be written.
     * @param data The data to be written to the file.
     * @throws IOException if an I/O error occurs while writing to the file.
     */
    private static void writeToFile(Path outputPath, String data) throws IOException {
        FileWriter writer = new FileWriter(outputPath.toFile());
        writer.write(data);
        writer.close();
    }

    /**
     * Calculates the production interval in microseconds (us) needed to achieve a given
     * bandwidth (in bits per second) over a variable packet size.
     *
     * @param packetSizeBytes the packet size in bytes
     * @param bandwidthBps   the desired bandwidth in bits per second
     * @return the production interval in microseconds
     */
    public static double calculateProductionInterval(double packetSizeBytes, double bandwidthBps) {
        return packetSizeBytes * 8 / (bandwidthBps / 1e6);
    }

    public static long getUDPBurstValueFromRaw(double frameSizeBit) {
        // We assume the original intention was to send a burst of traffic with a maximum MTU of 1500
        // (0) we find out how many frames that would have been.
        return (long)Math.ceil(((frameSizeBit / 8) / MAX_MTU_BYTES) * MAX_UDP_MSG_SIZE);
    }

    /**
     * Accounts for the UDP (User Datagram Protocol) overhead in the given packet size.
     * The function subtracts the size of IPv4 header (20 bytes) and UDP header (8 bytes) from the packet size in bits.
     *
     * @param pSizeByte The size of the packet in bytes.
     * @return The adjusted packet size after accounting for UDP overhead.
     */
    public static long accountForUDPOverhead(long pSizeByte) {
        return pSizeByte - /* ipv4 */ 20 - /* udp */ 8;
    }
}
