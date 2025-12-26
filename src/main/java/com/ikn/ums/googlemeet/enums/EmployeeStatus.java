package com.ikn.ums.googlemeet.enums;


public enum EmployeeStatus {
    ACTIVE, INACTIVE;

	public static EmployeeStatus from(String value) {
	    try {
	        return value == null ? null
	                : EmployeeStatus.valueOf(value.trim().toUpperCase());
	    } catch (IllegalArgumentException ex) {
	        return null;
	    }
	}
}

