package org.networkcalculus.dnc.utils;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import org.networkcalculus.dnc.curves.ArrivalCurve;
import org.networkcalculus.dnc.curves.ServiceCurve;
import org.networkcalculus.dnc.network.server_graph.Flow;
import org.networkcalculus.dnc.network.server_graph.Server;
import org.networkcalculus.dnc.network.server_graph.ServerGraph;
import org.networkcalculus.dnc.network.server_graph.Turn;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    public static String getServerIdentifier(Server srv) {
        return "Server" + srv.getId();
    }

    public static String getFlowIdentifier(Flow flow, boolean isStart) {
        return "Flow" + flow.getId() + (isStart ? "Start" : "End");
    }



    // todo wip: this is a prototype of the conversion function, code will be split into methods later on
    public static void convert(final ServerGraph sg) throws Exception {
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


        java.net.URL nedTemplate = OmnetConverter.class.getClassLoader().getResource("omnet/tsn_netgraph.jinja");
        if (nedTemplate == null) {
            throw new FileNotFoundException("omnet tsn netgraph template not found");
        }

        java.net.URL iniTemplate = OmnetConverter.class.getClassLoader().getResource("omnet/tsn_omnetpp.jinja");
        if (iniTemplate == null) {
            throw new FileNotFoundException("omnetpp ini template not found");
        }


        // Templating output starts here
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("devices", devices);
        ctx.put("connections", connections);
        ctx.put("sinks", sinks);
        ctx.put("sources", sources);

        JinjavaConfig conf = JinjavaConfig.newBuilder()
                .withLstripBlocks(true)
                .withTrimBlocks(true)
                .build();


        Jinjava jinjava = new Jinjava(conf);


        String nedOutput = jinjava.render(Files.readString(Paths.get(nedTemplate.toURI())), ctx);
        String iniOutput = jinjava.render(Files.readString(Paths.get(iniTemplate.toURI())), ctx);

        System.out.println(nedOutput);
        System.out.println(iniOutput);
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
