/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayInitialRoutesController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.controler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LegStartedIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.filter.TransportModeFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.modules.ReplanningModule;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

/**
 * Creates initial routes using within-day replanning. Doing so should hopefully
 * result in a more balanced traffic load in the network than creating all routes
 * initially on an empty network. 
 * 
 * Since routes are created within-day, we ensure that all plans contain at least
 * dummy routes, otherwise PersonPrepareForSim would create them, which is not
 * necessary but very time consuming.
 *  
 * @author cdobler
 */
public class WithinDayInitialRoutesController extends WithinDayController implements StartupListener {

	private DuringLegIdentifierFactory duringLegFactory;
	private DuringLegIdentifierFactory startedLegFactory;
	private DuringLegIdentifier legPerformingIdentifier;
	private DuringLegIdentifier legStartedIdentifier;
	
	private TransportModeFilterFactory carLegAgentsFilterFactory;
	
	private double duringLegReroutingShare = 0.10;
	
	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new WithinDayInitialRoutesController(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}
		
	public WithinDayInitialRoutesController(String[] args) {
		super(args);
		
		/*
		 * Change the persons' original plans. By doing so, the (hopefully) optimized
		 * routes are written to the output plans file.
		 */
		ExperimentalBasicWithindayAgent.copySelectedPlan = false;
		
		// register this as a Controller Listener
		super.addControlerListener(this);
	}

	@Override
	protected void loadData() {
		super.loadData();
		
		/*
		 * Create dummy car routes, if routes are not already present. By doing so,
		 * PersonPrepareForSim does not pre-calculate routes which would be deleted
		 * anyway when the agents depart.
		 */
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) this.getPopulation().getFactory()).getModeRouteFactory();
		DummyRoutesCreator dummyRoutesCreator = new DummyRoutesCreator(routeFactory);		
		dummyRoutesCreator.run(this.getPopulation());
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		/*
		 * Ensure that only a single iteration is performed.
		 */
		this.config.controler().setFirstIteration(0);
		this.config.controler().setLastIteration(0);
		
		/*
		 * Get number of threads from config file.
		 */
		int numReplanningThreads = this.config.global().getNumberOfThreads();
		
		/*
		 * Initialize TravelTimeCollector and create a FactoryWrapper which will act as
		 * factory but returns always the same travel time object, which is possible since
		 * the TravelTimeCollector is not personalized.
		 */
		Set<String> analyzedModes = new HashSet<String>();
		analyzedModes.add(TransportMode.car);
		super.createAndInitTravelTimeCollector(analyzedModes);
		
		/*
		 * Create and initialize replanning manager and replanning maps.
		 */
		super.initWithinDayEngine(numReplanningThreads);
		super.createAndInitLinkReplanningMap();
			
		// initialize Identifiers and Replanners
		this.initIdentifiers();
		this.initReplanners();
	}
	
	private void initIdentifiers() {
		
		/*
		 * During Leg Identifiers
		 */		
		Set<String> duringLegRerouteTransportModes = new HashSet<String>();
		duringLegRerouteTransportModes.add(TransportMode.car);
		
		carLegAgentsFilterFactory = new TransportModeFilterFactory(duringLegRerouteTransportModes);
		this.getQueueSimulationListener().add(carLegAgentsFilterFactory);

		duringLegFactory = new LeaveLinkIdentifierFactory(this.getLinkReplanningMap(), duringLegRerouteTransportModes); 
		this.legPerformingIdentifier = duringLegFactory.createIdentifier();
		
		startedLegFactory = new LegStartedIdentifierFactory(this.getLinkReplanningMap());
		startedLegFactory.addAgentFilterFactory(carLegAgentsFilterFactory);
		this.legStartedIdentifier = startedLegFactory.createIdentifier();
	}
	
	private void initReplanners() {
		
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) this.getPopulation().getFactory()).getModeRouteFactory();
								
		Map<String, TravelTime> travelTimes = new HashMap<String, TravelTime>();	
		travelTimes.put(TransportMode.car, this.getTravelTimeCalculator());
		
		// add time dependent penalties to travel costs within the affected area
		TravelDisutilityFactory disutilityFactory = this.getTravelDisutilityFactory();

		LeastCostPathCalculatorFactory factory = this.getLeastCostPathCalculatorFactory();
		AbstractMultithreadedModule router = new ReplanningModule(config, network, disutilityFactory, travelTimes, factory, routeFactory);
		
		/*
		 * During Leg Replanner
		 */
		WithinDayDuringLegReplannerFactory duringLegReplannerFactory;
		
		duringLegReplannerFactory = new CurrentLegReplannerFactory(this.scenarioData, this.getWithinDayEngine(), router, duringLegReroutingShare);
		duringLegReplannerFactory.addIdentifier(this.legPerformingIdentifier);
		this.getWithinDayEngine().addDuringLegReplannerFactory(duringLegReplannerFactory);
		
		duringLegReplannerFactory = new CurrentLegReplannerFactory(this.scenarioData, this.getWithinDayEngine(), router, 1.0);
		duringLegReplannerFactory.addIdentifier(this.legStartedIdentifier);
		this.getWithinDayEngine().addDuringLegReplannerFactory(duringLegReplannerFactory);
	}
	
	private static class DummyRoutesCreator extends AbstractPersonAlgorithm implements PlanAlgorithm {

		private final ModeRouteFactory routeFactory;
		
		public DummyRoutesCreator(ModeRouteFactory routeFactory) {
			this.routeFactory = routeFactory;
		}
		
		@Override
		public void run(Plan plan) {
			for (int i = 0; i < plan.getPlanElements().size(); i++) {
				PlanElement planElement = plan.getPlanElements().get(i);
				if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					if (leg.getMode().equals(TransportMode.car)) {
						if (leg.getRoute() == null) {
							Id startLinkId = ((Activity) plan.getPlanElements().get(i - 1)).getLinkId();
							Id endLinkId = ((Activity) plan.getPlanElements().get(i + 1)).getLinkId();
							leg.setRoute(routeFactory.createRoute(TransportMode.car, startLinkId, endLinkId));
						}
					}
				}
			}
		}

		@Override
		public void run(Person person) {
			for (Plan plan : person.getPlans()) {
				this.run(plan);
			}
		}
	}
	
}