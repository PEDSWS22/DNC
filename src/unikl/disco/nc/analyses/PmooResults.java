/*
 * This file is part of the Disco Deterministic Network Calculator v2.3.5 "Centaur".
 *
 * Copyright (C) 2005 - 2007 Frank A. Zdarsky
 * Copyright (C) 2008 - 2010 Andreas Kiefer
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

package unikl.disco.nc.analyses;

import java.util.Map;
import java.util.Set;

import unikl.disco.curves.ServiceCurve;
import unikl.disco.curves.ArrivalCurve;
import unikl.disco.nc.AnalysisResults;
import unikl.disco.network.Server;
import unikl.disco.numbers.Num;

/**
 * 
 * @author Frank A. Zdarsky
 * @author Andreas Kiefer
 * @author Steffen Bondorf
 * 
 */
public class PmooResults extends AnalysisResults {	
	protected Set<ServiceCurve> betas_e2e;

	protected PmooResults(){}
			
	protected PmooResults( Num delay_bound,
						  Num backlog_bound,
						  Set<ServiceCurve> betas_e2e,
						  Map<Server,Set<ArrivalCurve>> map__server__alphas ) {
		
		super( delay_bound, backlog_bound, map__server__alphas );
		
		this.betas_e2e = betas_e2e;
	}
	
	@Override
	protected void setDelayBound( Num delay_bound ) {
		super.setDelayBound( delay_bound );
	}
	
	@Override
	protected void setBacklogBound( Num backlog_bound ) {
		super.setBacklogBound( backlog_bound );
	}
}