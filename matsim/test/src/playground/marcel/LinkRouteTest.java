/* *********************************************************************** *
 * project: org.matsim.*
 * LinkRouteTest.java
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

package playground.marcel;

import org.matsim.population.Route;
import org.matsim.population.RouteImplTest;

import playground.marcel.LinkRoute;

/**
 * @author mrieser
 */
public class LinkRouteTest extends RouteImplTest {

	@Override
	public Route getRouteInstance() {
		return new LinkRoute();
	}

}
