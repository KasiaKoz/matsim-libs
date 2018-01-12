/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesConfigGroup.java
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

package org.matsim.core.config.groups;

import java.net.URL;
import java.util.Map;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author mrieser / Senozon AG
 */
public final class FacilitiesConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "facilities";

	private static final String INPUT_FILE= "inputFacilitiesFile";
	private static final String INPUT_FACILITY_ATTRIBUTES_FILE = "inputFacilityAttributesFile";
	private static final String INPUT_CRS = "inputCRS";

	private String inputFile = null;
	private String inputFacilitiesAttributesFile = null;
	private String inputCRS = null;

	private boolean creatingFacilities = true;

	private String idPrefix = "";
	private boolean oneFacilityPerLink = true;
	private boolean removingLinksAndCoordinates = true;
	private boolean assigningOpeningTime = false;
	private boolean assigningLinksToFacilitiesIfMissing = false;

	private static final String FACILITIES_SOURCE = "facilitiesSource";
	public enum FacilitiesSource {none, fromFile, onePerActivityLocationInPlansFile};
	private FacilitiesSource facilitiesSource = FacilitiesSource.none;

	private static final String ID_PREFIX="idPrefix";
	private static final String ONE_FACILITY_PER_LINK="oneFacilityPerLink";
	private static final String REMOVING_LINKS_AND_COORDINATES = "removingLinksAndCoordinates";
	private static final String ASSIGNING_OPENING_TIME = "assigningOpeningTime";
	private static final String ASSIGNING_LINKS_TO_FACILITIES_IF_MISSING="assigningLinksToFacilitiesIfMissing";

	public FacilitiesConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String,String> getComments() {
		final Map<String,String> comments = super.getComments();

		comments.put( INPUT_CRS , "The Coordinates Reference System in which the coordinates are expressed in the input file." +
				" At import, the coordinates will be converted to the coordinate system defined in \"global\", and will" +
				"be converted back at export. If not specified, no conversion happens." );

		{
			String options = "" ;
			for ( FacilitiesSource source : FacilitiesSource.values() ) {
				options += source + " " ;
			}
			comments.put(FACILITIES_SOURCE, "This defines how facilities should be created. Possible values: "+options);
		}

		comments.put( ID_PREFIX, "A prefix to be used in activityFacility id.");

		comments.put(ONE_FACILITY_PER_LINK, "Sets whether all activities on a link should be collected within one ActivityFacility." +
				" Default is 'true'. If set to 'false', for each coordinate found in the population's activities a separate ActivityFacility will be created.");

		comments.put(REMOVING_LINKS_AND_COORDINATES, "If set to 'true' (which is the default), the link and coordinate attributes " +
				"are nulled in the activities, as this information is now available via the facility.");

		comments.put(ASSIGNING_OPENING_TIME, "If set to 'true', opening time will be assigned to activity facilities from ActivityParams. Default is false.");

		comments.put(ASSIGNING_LINKS_TO_FACILITIES_IF_MISSING, "In the case that a facility has no link assigned, the ActivityFacility can be assigned to the closest link." +
				" If there should be only one ActivityFacility per link and if no link-assignment should be done, " +
				"then a new ActivityFacility will be created at that coordinate and the facility will not be assigned to a link.");
		return comments;
	}

	/* direct access */

	@StringGetter( INPUT_FILE )
	public String getInputFile() {
		return this.inputFile;
	}
	@StringSetter( INPUT_FILE )
	public void setInputFile(final String inputFile) {
		this.inputFile = inputFile;
	}

	public URL getInputFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, this.inputFile);
	}

	@StringGetter( INPUT_FACILITY_ATTRIBUTES_FILE )
	public String getInputFacilitiesAttributesFile() {
		return this.inputFacilitiesAttributesFile;
	}

	@StringSetter( INPUT_FACILITY_ATTRIBUTES_FILE )
	public void setInputFacilitiesAttributesFile(String inputFacilitiesAttributesFile) {
		this.inputFacilitiesAttributesFile = inputFacilitiesAttributesFile;
	}

	@StringGetter( INPUT_CRS )
	public String getInputCRS() {
		return inputCRS;
	}

	@StringSetter( INPUT_CRS )
	public void setInputCRS(String inputCRS) {
		this.inputCRS = inputCRS;
	}

	@StringGetter(ID_PREFIX)
	public String getIdPrefix() {
		return idPrefix;
	}

	@StringSetter(ID_PREFIX)
	public void setIdPrefix(String idPrefix) {
		this.idPrefix = idPrefix;
	}

	@StringGetter(ONE_FACILITY_PER_LINK)
	public boolean isOneFacilityPerLink() {
		return oneFacilityPerLink;
	}

	@StringSetter(ONE_FACILITY_PER_LINK)
	public void setOneFacilityPerLink(boolean oneFacilityPerLink) {
		this.oneFacilityPerLink = oneFacilityPerLink;
	}

	@StringGetter(REMOVING_LINKS_AND_COORDINATES)
	public boolean isRemovingLinksAndCoordinates() {
		return removingLinksAndCoordinates;
	}

	@StringSetter(REMOVING_LINKS_AND_COORDINATES)
	public void setRemovingLinksAndCoordinates(boolean removingLinksAndCoordinates) {
		this.removingLinksAndCoordinates = removingLinksAndCoordinates;
	}

	@StringGetter(ASSIGNING_OPENING_TIME)
	public boolean isAssigningOpeningTime() {
		return assigningOpeningTime;
	}

	@StringSetter(ASSIGNING_OPENING_TIME)
	public void setAssigningOpeningTime(boolean assigningOpeningTime) {
		this.assigningOpeningTime = assigningOpeningTime;
	}

	@StringGetter(ASSIGNING_LINKS_TO_FACILITIES_IF_MISSING)
	public boolean isAssigningLinksToFacilitiesIfMissing() {
		return assigningLinksToFacilitiesIfMissing;
	}

	@StringSetter(ASSIGNING_LINKS_TO_FACILITIES_IF_MISSING)
	public void setAssigningLinksToFacilitiesIfMissing(boolean assigningLinksToFacilitiesIfMissing) {
		this.assigningLinksToFacilitiesIfMissing = assigningLinksToFacilitiesIfMissing;
	}

	@StringGetter(FACILITIES_SOURCE)
	public FacilitiesSource getFacilitiesSource() {
		return this.facilitiesSource;
	}

	@StringSetter(FACILITIES_SOURCE)
	public void setFacilitiesSource(FacilitiesSource facilitiesSource) {
		this.facilitiesSource = facilitiesSource;
	}
}
