/*
 * This file is part of the Disco Deterministic Network Calculator v2.3.2 "Centaur".
 *
 * Copyright (C) 2005 - 2007 Frank A. Zdarsky
 * Copyright (C) 2011 - 2017 Steffen Bondorf
 *
 * Distributed Computer Systems (DISCO) Lab
 * University of Kaiserslautern, Germany
 *
 * http://disco.cs.uni-kl.de
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 */

package unikl.disco.nc.operations;

import unikl.disco.curves.Curve;
import unikl.disco.curves.ArrivalCurve;
import unikl.disco.curves.ServiceCurve;
import unikl.disco.numbers.Num;
import unikl.disco.numbers.NumFactory;
import unikl.disco.numbers.NumUtils;

/**
 * 
 * @author Frank A. Zdarsky
 * @author Steffen Bondorf
 *
 */
public class DelayBound {
	private static Num deriveForSpecialCurves( ArrivalCurve arrival_curve, ServiceCurve service_curve ) {
		if ( arrival_curve.equals( ArrivalCurve.createZeroArrival() ) ) {
			return NumFactory.createZero();
		}
		if ( service_curve.isDelayedInfiniteBurst() ) {
			// Assumption: the arrival curve does not have an initial latency.
			//             Otherwise its sub-additive closure would be zero, i.e., the arrival curve would not be sensible.
			return service_curve.getLatency().copy();
		}
		if ( service_curve.equals( ServiceCurve.createZeroService() )  // We know from above that the arrivals are not zero. 
				|| arrival_curve.getSustainedRate().gt( service_curve.getSustainedRate() ) ) {
			return NumFactory.createPositiveInfinity();
		}
		return null;
	}
	
	public static Num deriveARB( ArrivalCurve arrival_curve, ServiceCurve service_curve ) {
		Num result = deriveForSpecialCurves( arrival_curve, service_curve );
		if( result != null ) {
			return result;
		}
		
		return Curve.getXIntersection( arrival_curve, service_curve );
	}

	// Single flow to be bound, i.e., fifo per micro flow holds
	public static Num deriveFIFO( ArrivalCurve arrival_curve, ServiceCurve service_curve ) {

		Num result = deriveForSpecialCurves( arrival_curve, service_curve );
		if( result != null ) {
			return result;
		}
		
		result = NumFactory.createNegativeInfinity();
		for( int i = 0; i < arrival_curve.getSegmentCount(); i++ ) {
			Num ip_y = arrival_curve.getSegment( i ).getY();

			Num delay = NumUtils.sub( service_curve.f_inv( ip_y, true ), arrival_curve.f_inv( ip_y, false ) );
			result = NumUtils.max( result, delay );
		}
		for( int i = 0; i < service_curve.getSegmentCount(); i++ ) {
			Num ip_y = service_curve.getSegment( i ).getY();

			Num delay = NumUtils.sub( service_curve.f_inv( ip_y, true ), arrival_curve.f_inv( ip_y, false ) );
			result = NumUtils.max( result, delay );
		}
		
		return NumUtils.max( NumFactory.getZero(), result );
	}
}