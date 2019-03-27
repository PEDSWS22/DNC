/*
 * This file is part of the Deterministic Network Calculator (DNC).
 *
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

package org.networkcalculus.dnc.bounds;

import java.util.Set;

import org.networkcalculus.dnc.AnalysisConfig;
import org.networkcalculus.dnc.curves.ArrivalCurve;
import org.networkcalculus.dnc.curves.ServiceCurve;
import org.networkcalculus.dnc.network.server_graph.Path;
import org.networkcalculus.dnc.network.server_graph.Server;
import org.networkcalculus.num.Num;

public final class Bound {
    // --------------------------------------------------------------------------------------------------------------
    // Configuration
    // --------------------------------------------------------------------------------------------------------------
	
	private static final boolean FIFO_MUX_CHECKS = false;

	public static boolean exec_fifo_mux_checks() {
		return FIFO_MUX_CHECKS;
	}

	@Override
	public String toString() {
		if(FIFO_MUX_CHECKS) {
			return "FIFO checks";
		} else {
			return "All bound-operation checks are disabled";
		}
	}
	
    // --------------------------------------------------------------------------------------------------------------
    // Backlog
    // --------------------------------------------------------------------------------------------------------------

    public static Num backlog(ArrivalCurve arrival_curve, ServiceCurve service_curve) {
//        if (CalculatorConfig.getInstance().getOperationImpl().equals(CalculatorConfig.OperationImpl.DNC)
//                || CalculatorConfig.getInstance().getCurveImpl().equals(CalculatorConfig.CurveImpl.DNC)) {
//            return Backlog.derive(arrival_curve, service_curve);
//        }
        return Backlog.derive(arrival_curve,service_curve);

        // TODO: Sample of envisioned future solution, exemplified by (potentially incomplete) RTC MPA toolbox calls.
        /*
        Curve a = ((Curve_MPARTC_PwAffine) arrival_curve).getRtc_curve();
        Curve b = ((Curve_MPARTC_PwAffine) service_curve).getRtc_curve();

        double result = a.yXPlusEpsilon(0);

        // TODO: RTC equivalent for inflection points? There should be a better way
        ArrayList<Num> xcoords2 = CurvePwAffine.computeInflectionPointsX(arrival_curve, service_curve);
        ArrayList<Double> xcoords = new ArrayList<>();
        for (int i = 0; i < xcoords2.size(); i++) {
            xcoords.add(xcoords2.get(i).doubleValue());
        }
        for (int i = 0; i < xcoords.size(); i++) {
            double ip_x = xcoords.get(i);

            double backlog = a.yXPlusEpsilon(ip_x) - b.yXPlusEpsilon(ip_x);
            result = Math.max(result, backlog);
        }
        return Num.getFactory().create(result);
        */
    }
    
    // --------------------------------------------------------------------------------------------------------------
    // Delay
    // --------------------------------------------------------------------------------------------------------------

    public static Num delayARB(ArrivalCurve arrival_curve, ServiceCurve service_curve) {
//        if (CalculatorConfig.getInstance().getOperationImpl().equals(CalculatorConfig.OperationImpl.DNC)
//                || CalculatorConfig.getInstance().getCurveImpl().equals(CalculatorConfig.CurveImpl.DNC)) {
//            return Delay.deriveARB(arrival_curve, service_curve);
//        }
        return Delay.deriveARB(arrival_curve, service_curve);
    }

    public static Num delayFIFO(ArrivalCurve arrival_curve, ServiceCurve service_curve) {
//        if (CalculatorConfig.getInstance().getOperationImpl().equals(CalculatorConfig.OperationImpl.DNC)
//                || CalculatorConfig.getInstance().getCurveImpl().equals(CalculatorConfig.CurveImpl.DNC)) {
//            return Delay.deriveFIFO(arrival_curve, service_curve);
//        }
        return Delay.deriveFIFO(arrival_curve, service_curve);
    }

    // --------------------------------------------------------------------------------------------------------------
    // left-over Service
    // --------------------------------------------------------------------------------------------------------------

    public static Set<ServiceCurve> leftOverService(AnalysisConfig configuration, Server server,
                                               Set<ArrivalCurve> arrival_curves) {
//        if (CalculatorConfig.getInstance().getOperationImpl().equals(CalculatorConfig.OperationImpl.DNC)
//                || CalculatorConfig.getInstance().getCurveImpl().equals(CalculatorConfig.CurveImpl.DNC)) {
//            return LeftOverService.compute(configuration, server, arrival_curves);
//        }
        return LeftOverService.compute(configuration, server, arrival_curves);
    }

    public static Set<ServiceCurve> leftOverService(AnalysisConfig configuration, ServiceCurve service_curve,
                                               Set<ArrivalCurve> arrival_curves) {
//        if (CalculatorConfig.getInstance().getOperationImpl().equals(CalculatorConfig.OperationImpl.DNC)
//                || CalculatorConfig.getInstance().getCurveImpl().equals(CalculatorConfig.CurveImpl.DNC)) {
//            return LeftOverService.compute(configuration, service_curve, arrival_curves);
//        }
        return LeftOverService.compute(configuration, service_curve, arrival_curves);
    }

    public static Set<ServiceCurve> leftOverServiceFIFO(ServiceCurve service_curve, Set<ArrivalCurve> arrival_curves) {
//        if (CalculatorConfig.getInstance().getOperationImpl().equals(CalculatorConfig.OperationImpl.DNC)
//                || CalculatorConfig.getInstance().getCurveImpl().equals(CalculatorConfig.CurveImpl.DNC)) {
//            return LeftOverService.fifoMux(service_curve, arrival_curves);
//        }
        return LeftOverService.fifoMux(service_curve, arrival_curves);
    }

    public static ServiceCurve leftOverServiceFIFO(ServiceCurve service_curve, ArrivalCurve arrival_curve) {
//        if (CalculatorConfig.getInstance().getOperationImpl().equals(CalculatorConfig.OperationImpl.DNC)
//                || CalculatorConfig.getInstance().getCurveImpl().equals(CalculatorConfig.CurveImpl.DNC)) {
//            return LeftOverService.fifoMux(service_curve, arrival_curve);
//        }
        return LeftOverService.fifoMux(service_curve, arrival_curve);
    }

    public static Set<ServiceCurve> leftOverServiceARB(ServiceCurve service_curve, Set<ArrivalCurve> arrival_curves) {
//        if (CalculatorConfig.getInstance().getOperationImpl().equals(CalculatorConfig.OperationImpl.DNC)
//                || CalculatorConfig.getInstance().getCurveImpl().equals(CalculatorConfig.CurveImpl.DNC)) {
//            return LeftOverService.arbMux(service_curve, arrival_curves);
//        }
        return LeftOverService.arbMux(service_curve, arrival_curves);
    }

    public static ServiceCurve leftOverServiceARB(ServiceCurve service_curve, ArrivalCurve arrival_curve) {
//        if (CalculatorConfig.getInstance().getOperationImpl().equals(CalculatorConfig.OperationImpl.DNC)
//                || CalculatorConfig.getInstance().getCurveImpl().equals(CalculatorConfig.CurveImpl.DNC)) {
//            return LeftOverService.arbMux(service_curve, arrival_curve);
//        }
        return LeftOverService.arbMux(service_curve, arrival_curve);
    }

    // --------------------------------------------------------------------------------------------------------------
    // Output
    // --------------------------------------------------------------------------------------------------------------

    public static Set<ArrivalCurve> output(AnalysisConfig configuration, Set<ArrivalCurve> arrival_curves,
                                               Server server) throws Exception {
//        if (CalculatorConfig.getInstance().getOperationImpl().equals(CalculatorConfig.OperationImpl.DNC)
//                || CalculatorConfig.getInstance().getCurveImpl().equals(CalculatorConfig.CurveImpl.DNC)) {
//            return Output.compute(configuration, arrival_curves, server);
//        }
        return Output.compute(configuration, arrival_curves, server);
    }

    public static Set<ArrivalCurve> output(AnalysisConfig configuration, Set<ArrivalCurve> arrival_curves,
                                               Server server, Set<ServiceCurve> betas_lo) throws Exception {
//        if (CalculatorConfig.getInstance().getOperationImpl().equals(CalculatorConfig.OperationImpl.DNC)
//                || CalculatorConfig.getInstance().getCurveImpl().equals(CalculatorConfig.CurveImpl.DNC)) {
//            return Output.compute(configuration, arrival_curves, server, betas_lo);
//        }
        return Output.compute(configuration, arrival_curves, server, betas_lo);
    }

    public static Set<ArrivalCurve> output(AnalysisConfig configuration, Set<ArrivalCurve> arrival_curves, Path path,
                                               Set<ServiceCurve> betas_lo) throws Exception {
//        if (CalculatorConfig.getInstance().getOperationImpl().equals(CalculatorConfig.OperationImpl.DNC)
//                || CalculatorConfig.getInstance().getCurveImpl().equals(CalculatorConfig.CurveImpl.DNC)) {
//            return Output.compute(configuration, arrival_curves, path, betas_lo);
//        }
        return Output.compute(configuration, arrival_curves, path, betas_lo);
    }
}
