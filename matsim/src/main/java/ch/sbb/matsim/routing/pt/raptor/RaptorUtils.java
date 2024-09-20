/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;

/**
 * @author mrieser / SBB
 */
public final class RaptorUtils {


    private RaptorUtils() {
    }

    public static RaptorStaticConfig createStaticConfig(Config config) {
        PlansCalcRouteConfigGroup pcrConfig = config.plansCalcRoute();
        SwissRailRaptorConfigGroup srrConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);

        RaptorStaticConfig staticConfig = new RaptorStaticConfig();

        staticConfig.setBeelineWalkConnectionDistance(config.transitRouter().getMaxBeelineWalkConnectionDistance());

        PlansCalcRouteConfigGroup.ModeRoutingParams walk = pcrConfig.getModeRoutingParams().get(TransportMode.walk);
        staticConfig.setBeelineWalkSpeed(walk.getTeleportedModeSpeed() / walk.getBeelineDistanceFactor());
        staticConfig.setBeelineWalkDistanceFactor(walk.getBeelineDistanceFactor());
        staticConfig.setTransferWalkMargin(srrConfig.getTransferWalkMargin());
        staticConfig.setMinimalTransferTime(config.transitRouter().getAdditionalTransferTime());

        staticConfig.setUseModeMappingForPassengers(srrConfig.isUseModeMappingForPassengers());
        if (srrConfig.isUseModeMappingForPassengers()) {
            for (SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet mapping : srrConfig.getModeMappingForPassengers()) {
                staticConfig.addModeMappingForPassengers(mapping.getRouteMode(), mapping.getPassengerMode());
            }
        }
        staticConfig.setUseCapacityConstraints(srrConfig.isUseCapacityConstraints());

        return staticConfig;
    }

    public static RaptorParameters createParameters(Config config) {
        SwissRailRaptorConfigGroup advancedConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);

        TransitRouterConfig trConfig = new TransitRouterConfig(config);
        RaptorParameters raptorParams = new RaptorParameters(advancedConfig);
        raptorParams.setBeelineWalkSpeed(trConfig.getBeelineWalkSpeed());

        raptorParams.setSearchRadius(config.transitRouter().getSearchRadius());
        raptorParams.setExtensionRadius(config.transitRouter().getExtensionRadius());
        raptorParams.setDirectWalkFactor(config.transitRouter().getDirectWalkFactor());

        raptorParams.setMarginalUtilityOfWaitingPt_utl_s(trConfig.getMarginalUtilityOfWaitingPt_utl_s());

        PlanCalcScoreConfigGroup pcsConfig = config.planCalcScore();
        double marginalUtilityPerforming = pcsConfig.getPerforming_utils_hr() / 3600.0;
        for (Map.Entry<String, PlanCalcScoreConfigGroup.ModeParams> e : pcsConfig.getModes().entrySet()) {
            String mode = e.getKey();
            PlanCalcScoreConfigGroup.ModeParams modeParams = e.getValue();
            double marginalUtility_utl_s = modeParams.getMarginalUtilityOfTraveling()/3600.0 - marginalUtilityPerforming;
            raptorParams.setMarginalUtilityOfTravelTime_utl_s(mode, marginalUtility_utl_s);
        }
        
        double costPerHour = advancedConfig.getTransferPenaltyCostPerTravelTimeHour();
        if (costPerHour == 0.0) {
            // for backwards compatibility, use the default utility of line switch.
            raptorParams.setTransferPenaltyFixCostPerTransfer(-trConfig.getUtilityOfLineSwitch_utl());
        } else {
            raptorParams.setTransferPenaltyFixCostPerTransfer(advancedConfig.getTransferPenaltyBaseCost());
        }
        raptorParams.setTransferPenaltyPerTravelTimeHour(costPerHour);
        raptorParams.setTransferPenaltyMinimum(advancedConfig.getTransferPenaltyMinCost());
        raptorParams.setTransferPenaltyMaximum(advancedConfig.getTransferPenaltyMaxCost());

        return raptorParams;
    }

    public static List<? extends PlanElement> convertRouteToLegs(RaptorRoute route, double transferWalkMargin) {
        List<PlanElement> legs = new ArrayList<>(route.parts.size());
        double lastArrivalTime = Double.NaN;
        boolean firstPtLegProcessed = false;
        Leg previousTransferWalkleg = null;
        for (RaptorRoute.RoutePart part : route.parts) {
            if (part.planElements != null) {
                for (PlanElement pe : part.planElements) {
                    if (pe instanceof Leg) {
                        Leg leg = (Leg) pe;
                        legs.add(leg);
                        if (leg.getDepartureTime().isUndefined()) {
                            leg.setDepartureTime(lastArrivalTime);
                        }
                        lastArrivalTime = leg.getDepartureTime().seconds() + leg.getTravelTime().seconds();
                    }
                    else {
                    	Activity act = (Activity) pe;
                    	legs.add(act);
                    }
                }
            } else if (part.line != null) {
                // a pt leg
                Leg ptLeg = PopulationUtils.createLeg(part.mode);
                ptLeg.setDepartureTime(part.depTime);
                ptLeg.setTravelTime(part.arrivalTime - part.depTime);
                DefaultTransitPassengerRoute ptRoute = new DefaultTransitPassengerRoute(part.fromStop, part.line, part.route, part.toStop);
                ptRoute.setBoardingTime(part.boardingTime);
                ptRoute.setTravelTime(part.arrivalTime - part.depTime);
                ptRoute.setDistance(part.distance);
                ptLeg.setRoute(ptRoute);
                legs.add(ptLeg);
                lastArrivalTime = part.arrivalTime;
                firstPtLegProcessed = true;
                if (previousTransferWalkleg != null) {
                    //adds the margin only to legs in between pt legs
                    double traveltime = Math.max(0, previousTransferWalkleg.getTravelTime().seconds() - transferWalkMargin);
                    previousTransferWalkleg.setTravelTime(traveltime);
                    previousTransferWalkleg.getRoute().setTravelTime(traveltime);

                }
            } else {
                // a non-pt leg
                Leg walkLeg = PopulationUtils.createLeg(part.mode);
                walkLeg.setDepartureTime(part.depTime);
                double travelTime = part.arrivalTime - part.depTime;
                walkLeg.setTravelTime(travelTime);
                Id<Link> startLinkId = part.fromStop == null ? (route.fromFacility == null ? null : route.fromFacility.getLinkId()) : part.fromStop.getLinkId();
                Id<Link> endLinkId = part.toStop == null ? (route.toFacility == null ? null : route.toFacility.getLinkId()) : part.toStop.getLinkId();
                Route walkRoute = RouteUtils.createGenericRouteImpl(startLinkId, endLinkId);
                walkRoute.setTravelTime(travelTime);
                walkRoute.setDistance(part.distance);
                walkLeg.setRoute(walkRoute);
                legs.add(walkLeg);
                lastArrivalTime = part.arrivalTime;
                if (firstPtLegProcessed) {
                    previousTransferWalkleg = walkLeg;
                }
            }
        }

        return legs;
    }

    public static void recordLegChoices(RaptorRoute route, Person person, SwissRailRaptorData data) {
        int i = 0;
        for (RaptorRoute.RoutePart part : route.parts) {
            if (part.planElements != null) {
                for (PlanElement pe : part.planElements) {
                    if (pe instanceof Leg) {
                        Leg leg = (Leg) pe;
                        // todo soft-code car
                        if (leg.getMode().equals("car")) {
                            RaptorRoute.RoutePart nextRoutePart = routePartAtIndex(route, i + 1);
                            if (nextRoutePart != null && routePartIsPT(nextRoutePart)) {
                                // if there is a next route part, and it's PT, that means a tracked mode
                                // was used to access it, record the vehicle at the stop
                                data.parkVehicleAtStop(person, leg.getMode(), nextRoutePart.fromStop);
                            } else if (nextRoutePart != null) {
                                // there may be a transfer walk connecting that mode and PT
                                // we skip the transfer walk and look at the next route part
                                RaptorRoute.RoutePart nextNextRoutePart = routePartAtIndex(route, i + 2);
                                if (nextNextRoutePart != null && routePartIsPT(nextNextRoutePart)) {
                                    data.parkVehicleAtStop(person, leg.getMode(), nextNextRoutePart.fromStop);
                                }
                            } else {
                                // a tracked mode was used to egress a PT stop, release the vehicle from that stop
                                RaptorRoute.RoutePart prevRoutePart = routePartAtIndex(route, i - 1);
                                if (prevRoutePart != null && routePartIsPT(prevRoutePart)) {
                                    data.releaseVehicleFromStop(person, leg.getMode());
                                } else if (prevRoutePart != null) {
                                    // previous route part must have been a transfer walk, try an earlier route part
                                    RaptorRoute.RoutePart prevPrevRoutePart = routePartAtIndex(route, i - 2);
                                    if (prevPrevRoutePart != null && routePartIsPT(prevPrevRoutePart)) {
                                        data.releaseVehicleFromStop(person, leg.getMode());
                                    }
                                } else {
                                    // the tracked vehicle mode exists in isolation, which I don't think is possible
                                    // unless you configure walking as a tracked mode
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            i++;
        }
    }

    private static RaptorRoute.RoutePart routePartAtIndex(RaptorRoute route, int i) {
        if (route.parts.size() > i) {
            return route.parts.get(i);
        }
        return null;
    }

    private static Boolean routePartIsPT(RaptorRoute.RoutePart routePart) {
        return routePart.planElements == null && routePart.line != null;
    }

}
