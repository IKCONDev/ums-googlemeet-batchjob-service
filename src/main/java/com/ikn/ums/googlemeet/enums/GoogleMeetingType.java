package com.ikn.ums.googlemeet.enums;

/**
 * Represents the simplified meeting type classification used for Google Meet.
 *
 * Google meetings can be:
 *   - SINGLE_INSTANCE  - Non-recurring meetings
 *   - RECURRENCE       - Recurring master events
 *   - OCCURRENCE       - Individual occurrences of a recurring meeting
 */
public enum GoogleMeetingType {

//    SINGLE_INSTANCE("SINGLE_INSTANCE"),
//    RECURRENCE("RECURRENCE"),
//    OCCURRENCE("OCCURRENCE");
	
	
	 SINGLE_INSTANCE("singleInstance"),
	 RECURRENCE("occurrence");

    private final String value;

    GoogleMeetingType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
