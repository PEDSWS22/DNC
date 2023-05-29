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

import org.networkcalculus.dnc.AnalysisConfig;
import org.networkcalculus.dnc.CompFFApresets;
import org.networkcalculus.dnc.curves.ArrivalCurve;
import org.networkcalculus.dnc.curves.Curve;
import org.networkcalculus.dnc.curves.ServiceCurve;
import org.networkcalculus.dnc.network.server_graph.Flow;
import org.networkcalculus.dnc.network.server_graph.Server;
import org.networkcalculus.dnc.network.server_graph.ServerGraph;
import org.networkcalculus.dnc.network.server_graph.Turn;
import org.networkcalculus.dnc.omnet.OmnetConverter;
import org.networkcalculus.dnc.tandem.analyses.PmooAnalysis;

import java.util.LinkedList;

public class DemoOmnetST {

    public DemoOmnetST() {
    }

    public static void main(String[] args) {
        DemoOmnetST demo = new DemoOmnetST();

        try {
            demo.run();
        } catch (Exception e) {
        		e.printStackTrace();
        }
    }

    public void run() throws Exception {
        // Queuing + Processing?
        ServiceCurve service_curve = Curve.getFactory().createRateLatency(100e6, 0);

        // todo: should we use this latency as startTime of the

        ServerGraph sg = new ServerGraph();

        int numServers = 7;
        Server[] servers = new Server[numServers];

        for (int i = 0; i < numServers; i++) {
            servers[i] = sg.addServer(service_curve, AnalysisConfig.Multiplexing.FIFO);
            servers[i].useMaxSC(false);
            servers[i].useMaxScRate(false);
        }

        sg.addTurn(servers[0], servers[2]);
        sg.addTurn(servers[1], servers[2]);
        sg.addTurn(servers[3], servers[5]);
        sg.addTurn(servers[4], servers[5]);
        sg.addTurn(servers[2], servers[6]);
        sg.addTurn(servers[5], servers[6]);

        ArrivalCurve arrival_curve = Curve.getFactory().createTokenBucket(20e6, 1500*8*5);

        // adds foi and crossflows to the network
        Flow foi = sg.addFlow(arrival_curve, sg.getServer(0), sg.getServer(6));
        sg.addFlow(arrival_curve, sg.getServer(1), sg.getServer(6));
        sg.addFlow(arrival_curve, sg.getServer(3), sg.getServer(6));
        sg.addFlow(arrival_curve, sg.getServer(4), sg.getServer(6));

        double buffersize = Curve.getUtils().getMaxVerticalDeviation(arrival_curve, service_curve).doubleValue();
        // fixme: Use this buffer size to change the internal buffers

        System.out.println("Buffer size:" + buffersize);
        LinkedList<Turn> path0 = new LinkedList<Turn>();


        // Test conversion
        OmnetConverter omc = new OmnetConverter("/home/martb/Dokumente/OMNET/DNC/inet4.5/");
        omc.convert(sg, foi);
        // omc.setUnit.... mbps/bps

        CompFFApresets compffa_analyses = new CompFFApresets( sg );
        PmooAnalysis pmoo = compffa_analyses.pmoo_analysis;

        // Analyze the server graph
        // PMOO
        System.out.println("--- PMOO Analysis ---");
        try {
            pmoo.performAnalysis(foi);
            System.out.println("delay bound     : " + pmoo.getDelayBound());
        } catch (Exception e) {
            System.out.println("PMOO analysis failed");
            e.printStackTrace();
        }

    }
}