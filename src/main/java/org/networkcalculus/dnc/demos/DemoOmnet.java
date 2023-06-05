/*
 * This file is part of the Deterministic Network Calculator (DNC).
 *
 * Copyright (C) 2011 - 2018 Steffen Bondorf
 * Copyright (C) 2017 - 2018 The DiscoDNC contributors
 * Copyright (C) 2019+ The DNC contributors
 *
 * http://networkcalculus.org
 *
 *
 * The Deterministic Network Calculator (DNC) is free software;
 * you can redistribute it and/or modify it under the terms of the 
 * GNU Lesser General Public License as published by the Free Software Foundation; 
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package org.networkcalculus.dnc.demos;

import org.networkcalculus.dnc.CompFFApresets;
import org.networkcalculus.dnc.curves.*;
import org.networkcalculus.dnc.network.server_graph.Flow;
import org.networkcalculus.dnc.network.server_graph.Server;
import org.networkcalculus.dnc.network.server_graph.ServerGraph;
import org.networkcalculus.dnc.network.server_graph.Turn;
import org.networkcalculus.dnc.omnet.OmnetConverter;
import org.networkcalculus.dnc.tandem.analyses.PmooAnalysis;
import java.util.LinkedList;

public class DemoOmnet {

    public DemoOmnet() {
    }

    public static void main(String[] args) {
        DemoOmnet demo = new DemoOmnet();

        try {
            demo.run();
        } catch (Exception e) {
        		e.printStackTrace();
        }
    }

    public void run() throws Exception {
        // Queuing + Processing?
        ServiceCurve service_curve = Curve.getFactory().createRateLatency(50e6, 0.01);
        MaxServiceCurve max_service_curve = Curve.getFactory().createRateLatencyMSC(100.0e6, 0.001);

        ServerGraph sg = new ServerGraph();

        int numServers = 9;
        Server[] servers = new Server[numServers];

        for (int i = 1; i < numServers; i++) {
            servers[i] = sg.addServer(service_curve, max_service_curve);
            servers[i].useMaxSC(false);
            servers[i].useMaxScRate(false);
        }

        sg.addTurn(servers[1], servers[2]);
        Turn t_1_3 = sg.addTurn(servers[1], servers[3]);
        Turn t_1_2 = sg.addTurn(servers[1], servers[2]);
        Turn t_2_4 = sg.addTurn(servers[2], servers[4]);
        Turn t_3_4 = sg.addTurn(servers[3], servers[4]);
        Turn t_4_5 = sg.addTurn(servers[4], servers[5]);
        Turn t_5_6 = sg.addTurn(servers[5], servers[6]);
        Turn t_6_7 = sg.addTurn(servers[6], servers[7]);
        Turn t_7_8 = sg.addTurn(servers[7], servers[8]);

        // rate: 100.000, burst 10.000/8 = 1250,
        // fixme: burst is not a single packet its an amount of traffic
        // fixme: use maximum packet size internally but calculate burst by specifying a gate close/opening time.
        ArrivalCurve arrival_curve = Curve.getFactory().createTokenBucket(20e6, 1500*8);


        double buffersize = Curve.getUtils().getMaxVerticalDeviation(arrival_curve, service_curve).doubleValue();
        // fixme: Use this buffer size to change the internal buffers

        System.out.println("Buffer size:" + buffersize);
        LinkedList<Turn> path0 = new LinkedList<Turn>();

        // Turns need to be ordered from source server to sink server when defining a
        // path manually
        path0.add(t_2_4);
        path0.add(t_4_5);
        path0.add(t_5_6);
        path0.add(t_6_7);
        path0.add(t_7_8);


        LinkedList<Turn> path1 = new LinkedList<Turn>();
        path1.add(t_1_3);
        path1.add(t_3_4);
        path1.add(t_4_5);
        path1.add(t_5_6);


        Flow f0 = sg.addFlow(arrival_curve, path0);
        Flow f1 = sg.addFlow(arrival_curve, path1);

        // Test conversion
        OmnetConverter omc = new OmnetConverter("/home/martb/Documents/work/peds/omnetpp-6.0.1/samples/inet4.5/");
        omc.simulate(sg, f0);
        // omc.setUnit.... mbps/bps
    }
}