/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.vsp.parkAndRide.replanning;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

/**
 * A way of plugging park-and-ride strategy modules together. Via config file: <param name="Module_#" value="playground.vsp.parkAndRide.replanning.PRStrategyTime" />
 *  
 * @author ikaddoura
 *
 */
public class PRStrategyTime implements PlanStrategy {

	PlanStrategyImpl planStrategyDelegate = null ;
	
	public PRStrategyTime(Controler controler) {

		RandomPlanSelector planSelector = new RandomPlanSelector();
		planStrategyDelegate = new PlanStrategyImpl( planSelector );
				
		PRTimeAllocationMutator prTimeModule = new PRTimeAllocationMutator(controler.getConfig());
		planStrategyDelegate.addStrategyModule(prTimeModule);
		
		ReRoute reRouteModule = new ReRoute( controler.getScenario() ) ;
		planStrategyDelegate.addStrategyModule(reRouteModule) ;
		
	}

	@Override
	public void finish() {
		planStrategyDelegate.finish();
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		planStrategyDelegate.init(replanningContext);
	}

	@Override
	public void run(Person person) {
		planStrategyDelegate.run(person);
	}
}
