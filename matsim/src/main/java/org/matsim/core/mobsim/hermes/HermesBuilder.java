/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.AllowsConfiguration;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;
import org.matsim.core.router.util.TravelTime;

import java.util.*;

public class HermesBuilder implements AllowsConfiguration {
	private final Collection<AbstractQSimModule> qsimModules = new LinkedList<>();
	private final QSimComponentsConfig components = new QSimComponentsConfig();

	private final List<AbstractModule> overridingControllerModules = new LinkedList<>();
	private final List<AbstractQSimModule> overridingQSimModules = new LinkedList<>();
	private final Map<String, TravelTime> travelTimes = new HashMap<String, TravelTime>();

	@Override
	public HermesBuilder addOverridingModule(AbstractModule abstractModule) {
		overridingControllerModules.add(abstractModule);
		return this;
	}

	@Override
	public HermesBuilder addOverridingQSimModule(AbstractQSimModule qsimModule) {
		this.overridingQSimModules.add(qsimModule);
		return this;
	}

	@Override
	public HermesBuilder addQSimModule(AbstractQSimModule qsimModule) {
		this.qsimModules.add(qsimModule);
		return this;
	}

	@Override
	public HermesBuilder configureQSimComponents(QSimComponentsConfigurator configurator) {
		configurator.configure(components);
		return this;
	}

	public HermesBuilder addTravelTime(String mode, TravelTime travelTime) {
		this.travelTimes.put(mode, travelTime);
		return this;
	}

	public Hermes build(Scenario scenario, EventsManager eventsmanager) {
		Hermes hermes = new Hermes(scenario, eventsmanager);
		for (Map.Entry<String, TravelTime> entry: travelTimes.entrySet()){
			hermes.overrideTravelTimes(entry.getKey(), entry.getValue());
		}
		return hermes;
	}

}
