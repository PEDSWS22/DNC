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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
    private String identifier;
    private SubmoduleTypes type;

    private double servicerateLimit;

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
    private String name;
    private int port;

    private String flowid;

    OmnetUDPSink(String name, String flowid, int port) {
        this.name = name;
        this.flowid = flowid;
        this.port = port;
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
}

class OmnetSourceDestination {
    private String address;
    private int port;

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
    private final long pLen;
    private String name;
    private OmnetSourceDestination dest;

    private String flowid;

    OmnetSource(String name, OmnetSourceDestination dest, String flowid, long pLenB, double pIntervalUs) {
        this.name = name;
        this.dest = dest;
        this.flowid = flowid;
        this.pLen = pLenB;
        this.pInterval = pIntervalUs;
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

    public long getLength() {
        return pLen;
    }

    public String getFlowid() {
        return flowid;
    }
}



public class OmnetConverter {
    /**
     * The JinJava templating engine instance
     */
    Jinjava jinJava;

    public static String getServerIdentifier(Server srv) {
        return "Server" + srv.getId();
    }

    public static String getFlowIdentifier(Flow flow, boolean isStart) {
        return "Flow" + flow.getId() + (isStart ? "Start" : "End");
    }


    public static String getPrettyStringFromInputStream(InputStream stream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        return reader.lines().collect(Collectors.joining("\n"));
    }

    public OmnetConverter() {
        JinjavaConfig conf = JinjavaConfig.newBuilder()
        .withLstripBlocks(true)
        .withTrimBlocks(true)
        .build();

        this.jinJava = new Jinjava(conf);

    }

    public static File createTempFolder(String uuid) {
        try {
            Path folderPath = Paths.get("temp", "omnet-" + uuid);
            Files.createDirectories(folderPath);
            // Create a new folder
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

    public boolean compileSimulation(File workingDirectory, Path pathToInet) {
        String[] omnetMakeCommand = {
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
        };

        // First run the makefile generation
        if (!runCommand(omnetMakeCommand, workingDirectory)) {
            System.err.println("creating the make files for omnet failed");
            return false;
        }

        // Now compile the executable
        if (!runCommand(new String[]{"make"}, workingDirectory)) {
            System.err.println("compiling the simulation executable failed");
            return false;
        }

        // mark file as executable
        File file = Paths.get(workingDirectory.getAbsolutePath(), "simulation").toFile();
        if (!file.exists()) {
            System.err.println("simulation binary did not exist");
            return false;
        }

        file.setExecutable(true);
        return true;
    }

    public boolean runSimulation(File workingDirectory, Path pathToInet) {
        String[] omnetSimulationCommand = {
                "./simulation",
                "-m", "-u", "Cmdenv",
                 // Define the inet src folder so it finds the required NEDs
                "-n", ".:" + pathToInet.resolve("src"),
                "omnetpp.ini"
        };

        System.out.println(Arrays.toString(omnetSimulationCommand));

        return runCommand(omnetSimulationCommand, workingDirectory);
    }


    private boolean runCommand(String[] cmd, File workingDirectory) {
        // Create a new process builder and set the command
        ProcessBuilder builder = new ProcessBuilder(cmd);

        // Set the working directory
        builder.directory(workingDirectory);

        // Start the process
        try {
            Process process = builder.start();

            // todo: set up threaded streams to read stdout and redirect it to the console
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Running a command failed \n"
                        + getPrettyStringFromInputStream(process.getInputStream())
                        + "\n STDERR:" + getPrettyStringFromInputStream(process.getErrorStream())
                );

                return false;
            }

            System.out.println("Process exited with code " + exitCode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    // todo wip: this is a prototype of the conversion function, code will be split into methods later on
    public void convert(final ServerGraph sg) throws Exception {
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
            // Get the burst value (we assume bit), convert to byte and substract the "overhead"
            int pLenBit = (int) ((arrivalCurve.getBurst().doubleValue()) + 0.5);
            pLenBit = pLenBit - /* ethernet */ 64*8 - /* ipv4 */ 20*8 - /* udp */ 8*8;
            if (pLenBit <= 0) {
                throw new RuntimeException("Burst bit value too small to factor in real udp packet overhead, fix your model");
            }

            // Grab the rate in bit/s from the arrival curve and round up
            // fixme: Is this even the right "rate"?
            int rate = (int) (arrivalCurve.getUltAffineRate().doubleValue() + 0.5);

            // todo: If we had multiple sinks, we would need multiple ports
            int currentSinkPort = 1000;
            OmnetSource src = new OmnetSource(
                    srcID,
                    new OmnetSourceDestination(endFlowID, currentSinkPort),
                    monitorFlowID,
                    pLenBit,
                    calculateProductionInterval(pLenBit, rate)
            );

            List<OmnetSource> sourceList = sources.getOrDefault(startFlowID,  new LinkedList<>());
            sourceList.add(src);
            sources.put(startFlowID, sourceList);

            // Create a sink object that fits with the source
            OmnetUDPSink sink = new OmnetUDPSink(sinkID, monitorFlowID, currentSinkPort);
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
        ctx.put("max_time_s", 60);




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
                jinJava.render(Files.readString(Paths.get(iniTemplate.toURI())), ctx));
        } catch (IOException ex) {

        }


        // Compile the simulation files
        // todo: move this to its own function so we have a better api
        Path inetPath = Paths.get("/home/martb/Dokumente/OMNET/DNC/inet4.5/");
        compileSimulation(temporaryFolder, inetPath);
        runSimulation(temporaryFolder, inetPath);
    }

    private static void writeToFile(Path oututPath, String data) throws IOException {
        FileWriter writer = new FileWriter(oututPath.toFile());
        writer.write(data);
        writer.close();
    }

    /**
     * Calculates the production interval in microseconds (us) needed to achieve a given
     * bandwidth (in bits per second) over a variable packet size.
     *
     * @param packetSizeBit the packet size in bit
     * @param bandwidthBps   the desired bandwidth in bits per second
     * @return the production interval in microseconds
     */
    public static double calculateProductionInterval(double packetSizeBit, double bandwidthBps) {
        return packetSizeBit / (bandwidthBps / 1000000);
    }
}
