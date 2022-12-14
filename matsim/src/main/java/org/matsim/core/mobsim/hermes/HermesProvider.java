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
import org.matsim.core.mobsim.framework.Mobsim;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.core.router.util.TravelTime;

import java.util.Map;

public final class HermesProvider implements Provider<Mobsim> {

	private final Scenario scenario;
	private final EventsManager eventsManager;
	@Inject
	Map<String, TravelTime> travelTimes;

    @Inject
	public HermesProvider(Scenario scenario, EventsManager eventsManager, Map<String, TravelTime> travelTimes) {
        this.scenario = scenario;
        this.eventsManager = eventsManager;
	    this.travelTimes = travelTimes;
    }

	@Override
	public Mobsim get() {
		return new Hermes(scenario, eventsManager);
	}

}
