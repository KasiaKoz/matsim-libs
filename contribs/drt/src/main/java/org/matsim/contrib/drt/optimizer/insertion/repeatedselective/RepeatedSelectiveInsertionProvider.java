/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion.repeatedselective;

import com.google.common.annotations.VisibleForTesting;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.*;
import org.matsim.contrib.drt.optimizer.insertion.BestInsertionFinder.InsertionWithCost;
import static org.matsim.contrib.drt.optimizer.insertion.BestInsertionFinder.INSERTION_WITH_COST_COMPARATOR;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.stops.StopTimeCalculator;
import org.matsim.contrib.zone.skims.AdaptiveTravelTimeMatrix;
import org.matsim.core.router.util.TravelTime;

import static org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * @author steffenaxer
 */
class RepeatedSelectiveInsertionProvider {
    private static final double SPEED_FACTOR = 1.;
    private final InsertionCostCalculator insertionCostCalculator;
    private final InsertionGenerator insertionGenerator;
    private final ForkJoinPool forkJoinPool;
    @VisibleForTesting
	RepeatedSelectiveInsertionProvider(InsertionCostCalculator insertionCostCalculator,
									   InsertionGenerator insertionGenerator,
									   ForkJoinPool forkJoinPool) {
        this.insertionCostCalculator = insertionCostCalculator;
        this.insertionGenerator = insertionGenerator;
        this.forkJoinPool = forkJoinPool;
    }

    public static RepeatedSelectiveInsertionProvider create( InsertionCostCalculator insertionCostCalculator, AdaptiveTravelTimeMatrix updatableTravelTimeMatrix,
															TravelTime travelTime,
															ForkJoinPool forkJoinPool, StopTimeCalculator stopTimeCalculator) {
        var detourTimeEstimatorWithUpdatedTravelTimes = DetourTimeEstimatorWithAdaptiveTravelTimes.create(
                SPEED_FACTOR, updatableTravelTimeMatrix, travelTime);
        return new RepeatedSelectiveInsertionProvider(insertionCostCalculator,
                new InsertionGenerator(stopTimeCalculator, detourTimeEstimatorWithUpdatedTravelTimes), forkJoinPool);
    }

    public List<InsertionWithDetourData> getInsertions(DrtRequest drtRequest, Collection<VehicleEntry> vehicleEntries) {
        // Parallel outer stream over vehicle entries. The inner stream (flatmap) is
        // sequential.
        List<InsertionWithDetourData> preFilteredInsertions = forkJoinPool.submit(() -> vehicleEntries.parallelStream()
                // generate feasible insertions (wrt occupancy limits) with admissible detour
                // times
                .flatMap(e -> insertionGenerator.generateInsertions(drtRequest, e).stream())
                .map(i -> new InsertionWithCost(i,
                        this.insertionCostCalculator.calculate(drtRequest, i.insertion, i.detourTimeInfo)))
                // optimistic pre-filtering wrt admissible cost function (SPEED_FACTOR 1.0)
                .filter(iWithCost -> iWithCost.cost < INFEASIBLE_SOLUTION_COST)
                // sort all found insertions
                .sorted(INSERTION_WITH_COST_COMPARATOR)
                .map(iWithCost -> iWithCost.insertionWithDetourData)
                // collect
                .collect(Collectors.toList())).join();

        if (preFilteredInsertions.isEmpty()) {
            return List.of();
        }

        return preFilteredInsertions;
    }
}
