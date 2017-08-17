/*
 * This file is part of the Disco Deterministic Network Calculator v2.4.0beta3 "Chimera".
 *
 * Copyright (C) 2013 - 2017 Steffen Bondorf
 * Copyright (C) 2017 The DiscoDNC contributors
 *
 * Distributed Computer Systems (DISCO) Lab
 * University of Kaiserslautern, Germany
 *
 * http://disco.cs.uni-kl.de/index.php/projects/disco-dnc
 *
 *
 * The Disco Deterministic Network Calculator (DiscoDNC) is free software;
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

package de.uni_kl.cs.disco.tests;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import de.uni_kl.cs.disco.nc.Analysis.Analyses;
import de.uni_kl.cs.disco.nc.AnalysisConfig.Multiplexing;
import de.uni_kl.cs.disco.nc.analyses.PmooAnalysis;
import de.uni_kl.cs.disco.nc.analyses.SeparateFlowAnalysis;
import de.uni_kl.cs.disco.nc.analyses.TotalFlowAnalysis;
import de.uni_kl.cs.disco.network.Flow;
import de.uni_kl.cs.disco.network.Network;
import de.uni_kl.cs.disco.numbers.NumFactoryDispatch;

@RunWith(value = Parameterized.class)
public class TA_2S_1SC_4F_1AC_1P_Test extends DncTests {
    protected static final DncTestResults expected_results = new DncTestResults();
    protected static final DncTestResults expected_results_sinktree = new DncTestResults();
    private static TA_2S_1SC_4F_1AC_1P_Network test_network;
    private static Network network;
    private static Flow f0, f1, f2, f3;

    public TA_2S_1SC_4F_1AC_1P_Test(DncTestConfig test_config) throws Exception {
        super(test_config);
    }

    @BeforeClass
    public static void createNetwork() {
        test_network = new TA_2S_1SC_4F_1AC_1P_Network();
        f0 = test_network.f0;
        f1 = test_network.f1;
        f2 = test_network.f2;
        f3 = test_network.f3;

        network = test_network.getNetwork();

        initializeBounds();
    }

    private static void initializeBounds() {
        expected_results.clear();

        // TFA
        expected_results.setBounds(Analyses.TFA, Multiplexing.FIFO, f0, NumFactoryDispatch.create(36), NumFactoryDispatch.create(200));
        expected_results.setBounds(Analyses.TFA, Multiplexing.FIFO, f1, NumFactoryDispatch.create(36), NumFactoryDispatch.create(200));
        expected_results.setBounds(Analyses.TFA, Multiplexing.FIFO, f2, NumFactoryDispatch.create(36), NumFactoryDispatch.create(200));
        expected_results.setBounds(Analyses.TFA, Multiplexing.FIFO, f3, NumFactoryDispatch.create(36), NumFactoryDispatch.create(200));
        expected_results.setBounds(Analyses.TFA, Multiplexing.ARBITRARY, f0, NumFactoryDispatch.create(180), NumFactoryDispatch.create(200));
        expected_results.setBounds(Analyses.TFA, Multiplexing.ARBITRARY, f1, NumFactoryDispatch.create(180), NumFactoryDispatch.create(200));
        expected_results.setBounds(Analyses.TFA, Multiplexing.ARBITRARY, f2, NumFactoryDispatch.create(180), NumFactoryDispatch.create(200));
        expected_results.setBounds(Analyses.TFA, Multiplexing.ARBITRARY, f3, NumFactoryDispatch.create(180), NumFactoryDispatch.create(200));

        // SFA
        expected_results.setBounds(Analyses.SFA, Multiplexing.FIFO, f0, NumFactoryDispatch.create(34.5), NumFactoryDispatch.create(74));
        expected_results.setBounds(Analyses.SFA, Multiplexing.FIFO, f1, NumFactoryDispatch.create(34.5), NumFactoryDispatch.create(74));
        expected_results.setBounds(Analyses.SFA, Multiplexing.FIFO, f2, NumFactoryDispatch.create(34.5), NumFactoryDispatch.create(74));
        expected_results.setBounds(Analyses.SFA, Multiplexing.FIFO, f3, NumFactoryDispatch.create(34.5), NumFactoryDispatch.create(74));
        expected_results.setBounds(Analyses.SFA, Multiplexing.ARBITRARY, f0, NumFactoryDispatch.create(82.5), NumFactoryDispatch.create(170));
        expected_results.setBounds(Analyses.SFA, Multiplexing.ARBITRARY, f1, NumFactoryDispatch.create(82.5), NumFactoryDispatch.create(170));
        expected_results.setBounds(Analyses.SFA, Multiplexing.ARBITRARY, f2, NumFactoryDispatch.create(82.5), NumFactoryDispatch.create(170));
        expected_results.setBounds(Analyses.SFA, Multiplexing.ARBITRARY, f3, NumFactoryDispatch.create(82.5), NumFactoryDispatch.create(170));

        // PMOO
        expected_results.setBounds(Analyses.PMOO, Multiplexing.ARBITRARY, f0, NumFactoryDispatch.create(60), NumFactoryDispatch.create(125));
        expected_results.setBounds(Analyses.PMOO, Multiplexing.ARBITRARY, f1, NumFactoryDispatch.create(60), NumFactoryDispatch.create(125));
        expected_results.setBounds(Analyses.PMOO, Multiplexing.ARBITRARY, f2, NumFactoryDispatch.create(60), NumFactoryDispatch.create(125));
        expected_results.setBounds(Analyses.PMOO, Multiplexing.ARBITRARY, f3, NumFactoryDispatch.create(60), NumFactoryDispatch.create(125));

        // Sink-Tree PMOO at sink
        expected_results_sinktree.clear();

        expected_results_sinktree.setBounds(Analyses.PMOO, Multiplexing.ARBITRARY, f0, null, NumFactoryDispatch.create(200));
        expected_results_sinktree.setBounds(Analyses.PMOO, Multiplexing.ARBITRARY, f1, null, NumFactoryDispatch.create(200));
        expected_results_sinktree.setBounds(Analyses.PMOO, Multiplexing.ARBITRARY, f2, null, NumFactoryDispatch.create(200));
        expected_results_sinktree.setBounds(Analyses.PMOO, Multiplexing.ARBITRARY, f3, null, NumFactoryDispatch.create(200));
    }

    @Before
    public void reinitNetwork() {
        if (!super.reinitilize_test) {
            return;
        }

        test_network.reinitializeCurves();
        initializeBounds();
    }

    //--------------------Flow 0--------------------
    @Test
    public void f0_tfa() {
        setMux(network.getServers());
        super.runTFAtest(new TotalFlowAnalysis(network, test_config), f0, expected_results);
    }

    @Test
    public void f0_sfa() {
        setMux(network.getServers());
        super.runSFAtest(new SeparateFlowAnalysis(network, test_config), f0, expected_results);
    }

    @Test
    public void f0_pmoo_arbMux() {
        setArbitraryMux(network.getServers());
        super.runPMOOtest(new PmooAnalysis(network, test_config), f0, expected_results);
    }

    @Test
    public void f0_sinktree_arbMux() {
        setArbitraryMux(network.getServers());
        super.runSinkTreePMOOtest(network, f0, expected_results_sinktree);
    }

    //--------------------Flow 1--------------------
    @Test
    public void f1_tfa() {
        setMux(network.getServers());
        super.runTFAtest(new TotalFlowAnalysis(network, test_config), f1, expected_results);
    }

    @Test
    public void f1_sfa() {
        setMux(network.getServers());
        super.runSFAtest(new SeparateFlowAnalysis(network, test_config), f1, expected_results);
    }

    @Test
    public void f1_pmoo_arbMux() {
        setArbitraryMux(network.getServers());
        super.runPMOOtest(new PmooAnalysis(network, test_config), f1, expected_results);
    }

    @Test
    public void f1_sinktree_arbMux() {
        setArbitraryMux(network.getServers());
        super.runSinkTreePMOOtest(network, f1, expected_results_sinktree);
    }

    //--------------------Flow 2--------------------
    @Test
    public void f2_tfa() {
        setMux(network.getServers());
        super.runTFAtest(new TotalFlowAnalysis(network, test_config), f2, expected_results);
    }

    @Test
    public void f2_sfa() {
        setMux(network.getServers());
        super.runSFAtest(new SeparateFlowAnalysis(network, test_config), f2, expected_results);
    }

    @Test
    public void f2_pmoo_arbMux() {
        setArbitraryMux(network.getServers());
        super.runPMOOtest(new PmooAnalysis(network, test_config), f2, expected_results);
    }

    @Test
    public void f2_sinktree_arbMux() {
        setArbitraryMux(network.getServers());
        super.runSinkTreePMOOtest(network, f2, expected_results_sinktree);
    }

    //--------------------Flow 3--------------------
    @Test
    public void f3_tfa() {
        setMux(network.getServers());
        super.runTFAtest(new TotalFlowAnalysis(network, test_config), f3, expected_results);
    }

    @Test
    public void f3_sfa() {
        setMux(network.getServers());
        super.runSFAtest(new SeparateFlowAnalysis(network, test_config), f3, expected_results);
    }

    @Test
    public void f3_pmoo_arbMux() {
        setArbitraryMux(network.getServers());
        super.runPMOOtest(new PmooAnalysis(network, test_config), f3, expected_results);
    }

    @Test
    public void f3_sinktree_arbMux() {
        setArbitraryMux(network.getServers());
        super.runSinkTreePMOOtest(network, f3, expected_results_sinktree);
    }
}