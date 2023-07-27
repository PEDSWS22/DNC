package org.networkcalculus.dnc.omnet;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import org.networkcalculus.dnc.CompFFApresets;
import org.networkcalculus.dnc.curves.ArrivalCurve;
import org.networkcalculus.dnc.curves.ServiceCurve;
import org.networkcalculus.dnc.network.server_graph.Flow;
import org.networkcalculus.dnc.network.server_graph.Server;
import org.networkcalculus.dnc.network.server_graph.ServerGraph;
import org.networkcalculus.dnc.network.server_graph.Turn;
import org.networkcalculus.dnc.tandem.analyses.PmooAnalysis;

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
    TSN_SWITCH("TsnSwitch");

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
 * (1) A installation of Omnet in the system path, e.g. you should be able to run opp_makemake without specifying a path
 * (2) A valid path to the compiled version of the inet framework, must be a release version
 */
public class OmnetConverter {
    public static class SimResult {
        /** The end-to-end delay obtained from the simulation. */
        double e2e;

        public double getE2E() {
            return e2e;
        }

        public double getBound() {
            return bound;
        }

        /** The delay bound associated with the simulation result. */
        double bound;

        /**
         * Constructs a SimResult object with the provided end-to-end delay and bound values.
         *
         * @param e2e   The end-to-end delay value obtained from the simulation.
         * @param bound The delay bound associated with the simulation result.
         */
        SimResult(double e2e, double bound) {
            this.e2e = e2e;
            this.bound = bound;
        }
    }

    public static final long MAX_MTU_BYTES = 1500;
    public static long MAX_UDP_MSG_SIZE = accountForUDPOverhead(MAX_MTU_BYTES);

    // Default settings
    private static final long DEFAULT_SIMULATION_TIME_LIMIT = 5;
    // Constants
    public static final String SIMULATION_BINARY_NAME = "simulation";
    public static final String SIMULATION_TEMP_FOLDER = "temp";
    public static final String SIMULATION_NAME_PREFIX = "omnet-";
    public static final String SCALAR_RESULT_FILE = "data.sca";

    public static final String CSV_RESULT_FILE = "result.csv";

    /**
     * The JinJava templating engine instance
     */
    Jinjava jinJava;

    // Path to the inet installation, required for compiling and executing the simulation
    Path inetPath;

    // Show the simulation output by default
    boolean showSimulationOutput = true;


    // todo: make this configurable using a setter mechanism
    private final OMCSVHelper.SupportedFlowProps[] desiredCSVFlowProperties = OMCSVHelper.DEFAULT_FLOW_PROPS;
    private final OMCSVHelper.SupportedServerProps[] desiredCSVServerProperties = OMCSVHelper.DEFAULT_SERVER_PROPS;

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
     * @param flow    The flow object for which to retrieve the identifier.
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
     *
     * @param inetPath the path to a **compiled (release mode)** version of the inet framework.
     */
    public OmnetConverter(String inetPath) {
        this.jinJava = new Jinjava(JinjavaConfig.newBuilder()
                .withLstripBlocks(true)
                .withTrimBlocks(true)
                .build());
        this.inetPath = Paths.get(inetPath);
    }

    /**
     * Creates a folder for the simulation using the specified UUID.
     *
     * @param uuid The UUID to be used for creating the folder.
     * @return The created folder as a {@link File} object
     * @throws IOException if the folders could not be created or if the target is an existing file
     */
    public static File createSimulationFolder(String uuid) throws IOException {
        Path folderPath = Paths.get(SIMULATION_TEMP_FOLDER, SIMULATION_NAME_PREFIX + uuid);
        Files.createDirectories(folderPath);

        // re-use existing folders
        File folder = folderPath.toFile();
        boolean preExisting = folder.exists();
        if (preExisting && folder.isDirectory()) {
            return folder;
        } else if (preExisting) {
            throw new IOException("could not create temp folder, a file with the same name exists");
        } else {
            throw new IOException("could not create temp folder, unknown error");
        }
    }

    /**
     * Compiles the simulation by generating makefiles and compiling the executable.
     *
     * @param workingDirectory The working directory where the simulation should be compiled.
     * @return True if the simulation was successfully compiled, false otherwise.
     */
    public boolean compileSimulation(File workingDirectory) {
        // First run the makefile generation
        if (!runCommand(new String[]{
                "opp_makemake",
                "-f", "--deep",
                // Define the inet makefile variable that is used in a later step
                String.format("-KINET_PROJ=%s", inetPath),
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
                "-n", ".:" + inetPath.resolve("src"),
                "omnetpp.ini"
        }, workingDirectory, showSimulationOutput);
    }


    /**
     * Executes a command with the specified arguments in the specified working directory.
     *
     * @param cmd              The command and its arguments to be executed.
     * @param workingDirectory The working directory where the command should be executed.
     * @param withOutput       Determines whether the command output should be displayed in the console.
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

    /**
     * Simulates the network based on the given server graph and flow of interest.
     *
     * @param sg             The server graph.
     * @param flowOfInterest The flow of interest.
     * @param simTimeLimit   The simulation time limit
     * @return The result of the simulation as SimResult
     * @see SimResult
     * @throws IOException                         if the required templates are not found or the temp folder could not be created.
     * @throws ScaExtractor.ValueNotFoundException if the result value is not found after the simulation.
     */
    public SimResult simulate(final ServerGraph sg, final Flow flowOfInterest, long simTimeLimit) throws IOException, ScaExtractor.ValueNotFoundException {
        List<OmnetDevice> devices = new LinkedList<>();
        List<OmnetConnection> connections = new LinkedList<>();

        // First register our TSN Switches
        registerSwitches(sg, devices);

        // Create all the connections between the servers
        createConnections(sg, connections);

        // Add the required flows between the servers.
        Map<String, List<OmnetUDPSink>> sinks = new HashMap<>();
        Map<String, List<OmnetSource>> sources = new HashMap<>();
        addFlows(sg, flowOfInterest, devices, connections, sinks, sources);

        java.net.URL nedTemplate = getClass().getResource("tsn_netgraph.jinja");
        if (nedTemplate == null) {
            throw new FileNotFoundException("omnet tsn netgraph template not found");
        }

        java.net.URL iniTemplate = getClass().getResource("tsn_omnetpp.jinja");
        if (iniTemplate == null) {
            throw new FileNotFoundException("omnetpp ini template not found");
        }

        // Create the temporary simulation folder
        File simulationFolder = createSimulationFolder(String.valueOf(sg.hashCode()));

        // Write the required simulation files.
        try {
            // Create the jinja context
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("devices", devices);
            ctx.put("connections", connections);
            ctx.put("sinks", sinks);
            ctx.put("sources", sources);
            ctx.put("max_time_s", simTimeLimit);

            writeToFile(
                    Paths.get(simulationFolder.getPath(), "package.ned"),
                    jinJava.render(Files.readString(Paths.get(nedTemplate.getPath())), ctx)
            );

            writeToFile(
                    Paths.get(simulationFolder.getPath(), "omnetpp.ini"),
                    jinJava.render(Files.readString(Paths.get(iniTemplate.getPath())), ctx)
            );
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }

        // Try to compile the simulation
        if (!compileSimulation(simulationFolder)) {
            return null;
        }

        // If the simulation failed return, the function provides information
        if (!runSimulation(simulationFolder)) {
            return null;
        }

        // Extract the simulation e2e delay from the result file
        double sime2e = ScaExtractor.getSimulationEndToEndDelay(
                Paths.get(simulationFolder.getPath(), SCALAR_RESULT_FILE)
        );
        System.out.println("omnet simulation completed: " + sime2e);

        // Run the theoretical bound analysis for the flow of interest.
        double bound = runPmooAnalysis(sg, flowOfInterest);
        System.out.println("pmoo analysis completed: " + bound);

        // Save the results to a csv file
        try {
            saveResults(sg, flowOfInterest, sime2e, bound, simulationFolder);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }

        return new SimResult(sime2e, bound);
    }

    /**
     * Simulate with the default time limit
     *
     * @see OmnetConverter#simulate(ServerGraph, Flow, long)
     */
    public SimResult simulate(final ServerGraph sg, final Flow flowOfInterest) throws IOException, ScaExtractor.ValueNotFoundException {
        return simulate(sg, flowOfInterest, DEFAULT_SIMULATION_TIME_LIMIT);
    }

    /**
     * Runs the PMOO analysis for a specific flow of interest in the server graph.
     *
     * @param sg             The server graph.
     * @param flowOfInterest The flow of interest.
     * @return The delay bound obtained from the PMOO analysis.
     */
    private double runPmooAnalysis(ServerGraph sg, Flow flowOfInterest) {
        CompFFApresets compffa_analyses = new CompFFApresets(sg);
        PmooAnalysis pmoo = compffa_analyses.pmoo_analysis;

        // Get the theoretical bound from the PMOO analysis
        try {
            pmoo.performAnalysis(flowOfInterest);
        } catch (Exception e) {
            System.err.println("PMOO analysis failed");
            e.printStackTrace();
            return Double.NaN;
        }

        return pmoo.getDelayBound().doubleValue();
    }

    /**
     * Saves simulation results to a CSV file.
     * Format: `foi_id, omnet_e2e, pmoo_e2e, f_%d_{prop}, s_%d_{prop}`
     * Where `%d` are the object ids and `{prop}` is one or more supported props for the object.
     *
     * @param sg               The server graph.
     * @param foi              The flow of interest
     * @param sime2e           The end-to-end simulation delay.
     * @param delayBound       The delay bound.
     * @param simulationFolder The folder where the simulation files are stored.
     * @throws IOException if an I/O error occurs while saving the results.
     */
    private void saveResults(ServerGraph sg, Flow foi, double sime2e, double delayBound, File simulationFolder) throws IOException {
        File csvFile = Path.of(simulationFolder.getPath(), "result.csv").toFile();
        System.out.println("saving results to: ./" + csvFile.getPath());

        // Save the csv file
        OMCSVHelper.ToFile(csvFile, sg, foi, sime2e, delayBound, desiredCSVFlowProperties, desiredCSVServerProperties);
    }

    /**
     * Registers switches in the server graph.
     *
     * @param sg      The server graph.
     * @param devices The list of devices to populate.
     */
    private void registerSwitches(ServerGraph sg, List<OmnetDevice> devices) {
        for (final Server srv : sg.getServers()) {
            if (srv.useMaxSC()) {
                throw new RuntimeException("no idea how to support max service curves for now.");
            }

            ServiceCurve curve = srv.getServiceCurve();
            devices.add(
                    new OmnetDevice(
                            getServerIdentifier(srv),
                            SubmoduleTypes.TSN_SWITCH,
                            // todo: does this make sense?
                            // If the curve exists and has a limit apply it.
                            curve != null ? (int) (curve.getUltAffineRate().doubleValue() + 0.5) : 0
                    )
            );
        }
    }

    /**
     * Creates OMNeT connections between servers in the server graph.
     *
     * @param sg          The server graph.
     * @param connections The list of connections to populate.
     */
    private void createConnections(ServerGraph sg, List<OmnetConnection> connections) {
        for (Turn turn : sg.getTurns()) {
            connections.add(
                    new OmnetConnection(
                            getServerIdentifier(turn.getSource()),
                            getServerIdentifier(turn.getDest())
                    )
            );
        }
    }

    /**
     * Adds flows to the OMNeT simulation configuration.
     *
     * @param sg             The server graph.
     * @param flowOfInterest The flow of interest.
     * @param devices        The list of devices to populate.
     * @param connections    The list of connections to populate.
     * @param sinks          The map of sinks to populate.
     * @param sources        The map of sources to populate.
     */
    private void addFlows(ServerGraph sg, Flow flowOfInterest, List<OmnetDevice> devices,
                          List<OmnetConnection> connections, Map<String, List<OmnetUDPSink>> sinks,
                          Map<String, List<OmnetSource>> sources) {
        for (Flow flow : sg.getFlows()) {
            boolean isFoi = flow.equals(flowOfInterest);
            LinkedList<Server> servers = flow.getServersOnPath();

            String startFlowID = getFlowIdentifier(flow, true);
            Server startServer = servers.getFirst();
            devices.add(new OmnetDevice(startFlowID, SubmoduleTypes.TSN_DEVICE, 0));
            connections.add(new OmnetConnection(
                    startFlowID,
                    getServerIdentifier(startServer)
            ));

            String endFlowID = getFlowIdentifier(flow, false);
            Server endServer = servers.getLast();
            devices.add(new OmnetDevice(endFlowID, SubmoduleTypes.TSN_DEVICE, 0));
            connections.add(new OmnetConnection(
                    endFlowID,
                    getServerIdentifier(endServer)
            ));

            String monitorFlowID = "f" + flow.getId();
            String srcID = monitorFlowID + "Source";
            String sinkID = monitorFlowID + "Sink";

            ArrivalCurve arrivalCurve = flow.getArrivalCurve();

            // fixme: this might be too static for certain network topologies
            int currentAppPort = 1000;
            OmnetSource src = new OmnetSource(
                    srcID,
                    new OmnetSourceDestination(endFlowID, currentAppPort),
                    monitorFlowID,
                    getUDPBurstValueFromRaw(arrivalCurve.getBurst().doubleValue()),
                    calculateProductionInterval(
                            MAX_UDP_MSG_SIZE,
                            (int) (arrivalCurve.getUltAffineRate().doubleValue() + 0.5)
                    ),
                    isFoi
            );

            List<OmnetSource> sourceList = sources.getOrDefault(startFlowID, new LinkedList<>());
            sourceList.add(src);
            sources.put(startFlowID, sourceList);

            OmnetUDPSink sink = new OmnetUDPSink(sinkID, monitorFlowID, currentAppPort, isFoi);
            List<OmnetUDPSink> appList = sinks.getOrDefault(endFlowID, new LinkedList<>());
            appList.add(sink);
            sinks.put(endFlowID, appList);
        }
    }

    /**
     * Writes the provided data to a file at the specified output path.
     *
     * @param outputPath The path where the file should be written.
     * @param data       The data to be written to the file.
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
     * @param bandwidthBps    the desired bandwidth in bits per second
     * @return the production interval in microseconds
     */
    public static double calculateProductionInterval(double packetSizeBytes, double bandwidthBps) {
        return packetSizeBytes * 8 / (bandwidthBps / 1e6);
    }

    public static long getUDPBurstValueFromRaw(double frameSizeBit) {
        // We assume the original intention was to send a burst of traffic with a maximum MTU of 1500
        // (0) we find out how many frames that would have been.
        return (long) Math.ceil(((frameSizeBit / 8) / MAX_MTU_BYTES) * MAX_UDP_MSG_SIZE);
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
