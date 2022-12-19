/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.hermes;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.ParallelEventsManager;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * @author dgrether
 */
public class TravelTimeTest {
	@Rule
	public TemporaryFolder tmpDir = new TemporaryFolder();

	@Test
	public void testEquilOneAgent() {
		Map<Id<Vehicle>, Map<Id<Link>, Double>> agentTravelTimes = new HashMap<>();

		Config config = ConfigUtils.loadConfig("test/scenarios/equil/config.xml");
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);

		String popFileName = "plans1.xml";
		config.plans().setInputFile(popFileName);

		ScenarioUtils.loadScenario(scenario);

		EventsManager events = new ParallelEventsManager(false);
		events.addHandler(new EventTestHandler(agentTravelTimes));

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new HermesBuilder() //
			.build(scenario, events) //
			.run();

		Map<Id<Link>, Double> travelTimes = agentTravelTimes.get(Id.create("1", Vehicle.class));
		Assert.assertEquals(358.0, travelTimes.get(Id.create(6, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(180.0, travelTimes.get(Id.create(15, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		// this one is NOT a travel time (it includes two activities and a zero-length trip)
		Assert.assertEquals(13558.0, travelTimes.get(Id.create(20, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(358.0, travelTimes.get(Id.create(21, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1251.0, travelTimes.get(Id.create(22, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(358.0, travelTimes.get(Id.create(23, Link.class)).intValue(), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testBindingCustomTravelTime() throws IOException {
		Config config = ConfigUtils.loadConfig("test/scenarios/equil/config.xml");
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);

		String popFileName = "plans1.xml";
		config.plans().setInputFile(popFileName);
		String outputDir = String.valueOf(tmpDir.newFolder("outputDir"));
		config.controler().setOutputDirectory(outputDir);
		config.controler().setMobsim("hermes");

		ScenarioUtils.loadScenario(scenario);
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new CustomTravelTimeModule());
		controler.run();

		Map<Id<Vehicle>, Map<Id<Link>, Double>> agentTravelTimes = new HashMap<>();
		EventsManager events = new ParallelEventsManager(false);
		events.addHandler(new EventTestHandler(agentTravelTimes));
		events.initProcessing();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(outputDir + "/output_events.xml.gz");
		events.finishProcessing();

		Map<Id<Link>, ? extends Link> matsimLinks = scenario.getNetwork().getLinks();
		Map<Id<Link>, Double> travelTimes = agentTravelTimes.get(Id.create("1", Vehicle.class));
		// checking uncongested links
		for (int i: Arrays.asList(6, 15, 21, 22)) {
			Id<Link> linkId = Id.create(i, Link.class);
			int length = (int) matsimLinks.get(linkId).getLength();
			int traveltime = travelTimes.get(linkId).intValue();
			Assert.assertEquals(length + 1, traveltime, MatsimTestUtils.EPSILON);
		}
	}

	@Test
	/**
	 * This test shows that the Netsim always rounds up link travel times.
	 * Please note that a computed link travel time of 400.0s is treated the same,
	 * i.e. it is rounded up to 401s.
	 */
	public void testEquilOneAgentTravelTimeRounding() {
		Map<Id<Vehicle>, Map<Id<Link>, Double>> agentTravelTimes = new HashMap<>();

		Config config = ConfigUtils.loadConfig("test/scenarios/equil/config.xml");
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);

		String popFileName = "plans1.xml";
		config.plans().setInputFile(popFileName);

		ScenarioUtils.loadScenario(scenario);

		EventsManager events = new ParallelEventsManager(false);
		events.addHandler(new EventTestHandler(agentTravelTimes));

		// Travel time 359.9712023038
		scenario.getNetwork().getLinks().get(Id.createLinkId("6")).setFreespeed(27.78);
		ScenarioImporter.flush();

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new HermesBuilder() //
			.build(scenario, events) //
			.run();

		Map<Id<Link>, Double> travelTimes = agentTravelTimes.get(Id.create("1", Vehicle.class));
		Assert.assertEquals(358.0, travelTimes.get(Id.create(6, Link.class)).intValue(), MatsimTestUtils.EPSILON);

		// Travel time 359.9712023038
		scenario.getNetwork().getLinks().get(Id.createLinkId("6")).setFreespeed(27.85);
		ScenarioImporter.flush();

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new HermesBuilder() //
			.build(scenario, events) //
			.run();

		travelTimes = agentTravelTimes.get(Id.create("1", Vehicle.class));
		Assert.assertEquals(358.0, travelTimes.get(Id.create(6, Link.class)).intValue(), MatsimTestUtils.EPSILON);

		// Travel time 359.066427289
		scenario.getNetwork().getLinks().get(Id.createLinkId("6")).setFreespeed(27.85);
		ScenarioImporter.flush();

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new HermesBuilder() //
			.build(scenario, events) //
			.run();

		travelTimes = agentTravelTimes.get(Id.create("1", Vehicle.class));
		Assert.assertEquals(358.0, travelTimes.get(Id.create(6, Link.class)).intValue(), MatsimTestUtils.EPSILON);

		// Travel time 358.4229390681
		scenario.getNetwork().getLinks().get(Id.createLinkId("6")).setFreespeed(27.9);
		ScenarioImporter.flush();

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new HermesBuilder() //
			.build(scenario, events) //
			.run();

		travelTimes = agentTravelTimes.get(Id.create("1", Vehicle.class));
		Assert.assertEquals(358.0, travelTimes.get(Id.create(6, Link.class)).intValue(), MatsimTestUtils.EPSILON);

		// Travel time 360.3603603604
		scenario.getNetwork().getLinks().get(Id.createLinkId("6")).setFreespeed(27.75);
		ScenarioImporter.flush();

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new HermesBuilder() //
			.build(scenario, events) //
			.run();

		travelTimes = agentTravelTimes.get(Id.create("1", Vehicle.class));
		Assert.assertEquals(358.0, travelTimes.get(Id.create(6, Link.class)).intValue(), MatsimTestUtils.EPSILON);

		// Travel time 400.0
		scenario.getNetwork().getLinks().get(Id.createLinkId("6")).setLength(10000.0);
		scenario.getNetwork().getLinks().get(Id.createLinkId("6")).setFreespeed(25.0);
		ScenarioImporter.flush();

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new HermesBuilder() //
			.build(scenario, events) //
			.run();

		travelTimes = agentTravelTimes.get(Id.create("1", Vehicle.class));
		Assert.assertEquals(401.0, travelTimes.get(Id.create(6, Link.class)).intValue(), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testEquilTwoAgents() {
		Map<Id<Vehicle>, Map<Id<Link>, Double>> agentTravelTimes = new HashMap<>();

		Config config = ConfigUtils.loadConfig("test/scenarios/equil/config.xml");
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);

		String popFileName = "plans2.xml";
		config.plans().setInputFile(popFileName);

		ScenarioUtils.loadScenario(scenario);

		EventsManager events = new ParallelEventsManager(false);
		events.addHandler(new EventTestHandler(agentTravelTimes));

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new HermesBuilder() //
			.build(scenario, events) //
			.run();

		Map<Id<Link>, Double> travelTimes = agentTravelTimes.get(Id.create("1", Vehicle.class));
		Assert.assertEquals(358.0, travelTimes.get(Id.create(6, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(180.0, travelTimes.get(Id.create(15, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		// this one is NOT a travel time (it includes two activities and a zero-length trip)
		Assert.assertEquals(13558.0, travelTimes.get(Id.create(20, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(358.0, travelTimes.get(Id.create(21, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1251.0, travelTimes.get(Id.create(22, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(358.0, travelTimes.get(Id.create(23, Link.class)).intValue(), MatsimTestUtils.EPSILON);


		travelTimes = agentTravelTimes.get(Id.create("2", Vehicle.class));
		Assert.assertEquals(358.0, travelTimes.get(Id.create(5, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(180.0, travelTimes.get(Id.create(14, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		// this one is NOT a travel time (it includes two activities and a zero-length trip)
		Assert.assertEquals(13558.0, travelTimes.get(Id.create(20, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(358.0, travelTimes.get(Id.create(21, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1251.0, travelTimes.get(Id.create(22, Link.class)).intValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(358.0, travelTimes.get(Id.create(23, Link.class)).intValue(), MatsimTestUtils.EPSILON);
	}


	private static class EventTestHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicleTravelTimes;

		public EventTestHandler(Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicleTravelTimes) {
			this.vehicleTravelTimes = vehicleTravelTimes;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Map<Id<Link>, Double> travelTimes = this.vehicleTravelTimes.get(event.getVehicleId());
			if (travelTimes == null) {
				travelTimes = new HashMap<>();
				this.vehicleTravelTimes.put(event.getVehicleId(), travelTimes);
			}
			travelTimes.put(event.getLinkId(), event.getTime());
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<Id<Link>, Double> travelTimes = this.vehicleTravelTimes.get(event.getVehicleId());
			if (travelTimes != null) {
				Double d = travelTimes.get(event.getLinkId());
				if (d != null) {
					double time = event.getTime() - d;
					travelTimes.put(event.getLinkId(), time);
				}
			}
		}

		@Override
		public void reset(int iteration) {
		}
	}

	public static class CustomTravelTimeModule extends AbstractModule {

		@Override
		public void install() {
			addTravelTimeBinding("car").to(CustomTravelTime.class).in(Singleton.class);
			this.bindMobsim().toProvider(CustomHermesProvider.class);
		}
	}

	public static class CustomTravelTime implements TravelTime {

		@Override
		public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			return 1.0;
		}
	}

	public static class CustomQSimModule extends AbstractQSimModule {
		static final public String COMPONENT_NAME = "Custom";

		private final Map<String, TravelTime> customTravelTimes;

		public CustomQSimModule(Map<String, TravelTime> customTravelTimes) {
			this.customTravelTimes = customTravelTimes;
		}

		@Override
		protected void configureQSim() {
//			addNamedComponent(MultiModalSimEngine.class, COMPONENT_NAME);
//			addNamedComponent(MultiModalDepartureHandler.class, COMPONENT_NAME);
		}
	}

	public static class CustomHermesProvider implements Provider<Mobsim> {
		private Scenario scenario;
		private EventsManager eventsManager;
		private Map<String, TravelTime> customTravelTimes;

		@Inject
		CustomHermesProvider(Scenario scenario, EventsManager eventsManager) {
			this.scenario = scenario;
			this.eventsManager = eventsManager;
		}
		@Override
		public Mobsim get() {
			return new HermesBuilder().addTravelTime("car", new CustomTravelTime()).build(scenario, eventsManager);
		}
	}

}
