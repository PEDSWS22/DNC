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

import org.networkcalculus.dnc.curves.ArrivalCurve;
import org.networkcalculus.dnc.curves.Curve;
import org.networkcalculus.dnc.curves.MaxServiceCurve;
import org.networkcalculus.dnc.curves.ServiceCurve;
import org.networkcalculus.dnc.network.server_graph.Flow;
import org.networkcalculus.dnc.network.server_graph.Server;
import org.networkcalculus.dnc.network.server_graph.ServerGraph;
import org.networkcalculus.dnc.network.server_graph.Turn;
import org.networkcalculus.dnc.omnet.OmnetConverter;

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
        ServiceCurve service_curve = Curve.getFactory().createRateLatency(50e6, 0.01);
        MaxServiceCurve max_service_curve = Curve.getFactory().createRateLatencyMSC(100.0e6, 0.001);

        ServerGraph sg = new ServerGraph();

        int numServers = 2;
        Server[] servers = new Server[numServers];

        for (int i = 0; i < numServers; i++) {
            servers[i] = sg.addServer(service_curve, max_service_curve);
            servers[i].useMaxSC(false);
            servers[i].useMaxScRate(false);
        }

        Turn t_1_2 = sg.addTurn(servers[0], servers[1]);

        // rate: 100.000, burst 10.000/8 = 1250,
        ArrivalCurve arrival_curve = Curve.getFactory().createTokenBucket(20e6, 1500*8);

        LinkedList<Turn> path0 = new LinkedList<>();

        // Turns need to be ordered from source server to sink server when defining a path manually
        path0.add(t_1_2);

        Flow f0 = sg.addFlow(arrival_curve, path0);

        // Test conversion
        OmnetConverter omc = new OmnetConverter("/home/martb/Dokumente/OMNET/DNC/inet4.5");
        OmnetConverter.SimResult res = omc.simulate(sg, f0);
        System.out.printf("Simulation completed with e2e: %,.4f, bound: %,.4f\n", res.getE2E(), res.getBound());
    }
}