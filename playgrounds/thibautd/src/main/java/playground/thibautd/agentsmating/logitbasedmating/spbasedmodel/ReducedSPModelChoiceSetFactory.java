/* *********************************************************************** *
 * project: org.matsim.*
 * ReducedModelChoiceSetFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.agentsmating.logitbasedmating.spbasedmodel;

import java.util.List;

import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.agentsmating.logitbasedmating.framework.Alternative;
import playground.thibautd.agentsmating.logitbasedmating.framework.ChoiceSetFactory;
import playground.thibautd.agentsmating.logitbasedmating.framework.DecisionMaker;

/**
 * @author thibautd
 */
public class ReducedSPModelChoiceSetFactory implements ChoiceSetFactory {

	@Override
	public List<Alternative> createChoiceSet(
			final DecisionMaker decisionMaker,
			final Plan plan,
			final int indexOfleg) {
		// TODO Auto-generated method stub
		return null;
	}
}

