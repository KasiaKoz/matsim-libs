/* *********************************************************************** *
 * project: org.matsim.*
 * OptimisticTravelTimeAggregator.java
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

package org.matsim.trafficmonitoring;

public class OptimisticTravelTimeAggregator extends AbstractTravelTimeAggregator {

	public OptimisticTravelTimeAggregator(int numSlots, int travelTimeBinSize) {
		super(numSlots, travelTimeBinSize);
	}


	@Override
	protected void addTravelTime(TravelTimeRole travelTimeRole,
			double enterTime, double leaveTime) {

		final int timeSlot = getTimeSlotIndex(enterTime);
		travelTimeRole.addTravelTime(timeSlot, leaveTime - enterTime);
	
	}

	

}
