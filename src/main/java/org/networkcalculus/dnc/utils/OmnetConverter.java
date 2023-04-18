package org.networkcalculus.dnc.utils;

import com.hubspot.jinjava.Jinjava;
import org.networkcalculus.dnc.network.server_graph.Flow;
import org.networkcalculus.dnc.network.server_graph.Server;
import org.networkcalculus.dnc.network.server_graph.ServerGraph;
import org.networkcalculus.dnc.network.server_graph.Turn;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

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

    OmnetDevice(String identifier, SubmoduleTypes type) {
        this.identifier = identifier;
        this.type = type;
    }

    public SubmoduleTypes getType() {
        return type;
    }

    public String getIdentifier() {
        return identifier;
    }
}

enum SubmoduleTypes {
    TSN_DEVICE("TsnDevice"),
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


public class OmnetConverter {

    public static String getServerIdentifier(Server srv) {
        return "Server" + srv.getId();
    }

    public static String getFlowIdentifier(Flow flow, boolean isStart) {
        return "Flow" + flow.getId() + (isStart ? "Start" : "End");
    }

    public static void convert(final ServerGraph sg) throws Exception {
        List<OmnetDevice> devices = new ArrayList<>();

        for (final Server srv : sg.getServers() ) {
            devices.add(new OmnetDevice(getServerIdentifier(srv), SubmoduleTypes.TSN_SWITCH));
        }

        List<OmnetConnection> connections = new ArrayList<>();
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
            devices.add(new OmnetDevice(startFlowID, SubmoduleTypes.TSN_DEVICE));
            connections.add(new OmnetConnection(
                    startFlowID,
                    getServerIdentifier(startServer)
            ));

            // The flow target
            String endFlowID = getFlowIdentifier(flow, false);
            Server endServer = servers.getLast();
            devices.add(new OmnetDevice(endFlowID, SubmoduleTypes.TSN_DEVICE));
            connections.add(new OmnetConnection(
                    endFlowID,
                    getServerIdentifier(endServer)
            ));
        }



        // Templating output starts here
        Jinjava jinjava = new Jinjava();
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("devices", devices);
        ctx.put("connections", connections);


        java.net.URL res = OmnetConverter.class.getClassLoader().getResource("omnet/tsn_netgraph.jinja");
        if (res == null) {
            throw new FileNotFoundException("omnet tsn netgraph template not found");
        }

        String template = Files.readString(Paths.get(res.toURI()));

        String render = jinjava.render(template, ctx);
        System.out.println(render);
    }
}
