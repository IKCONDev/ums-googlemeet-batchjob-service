package com.ikn.ums.googlemeet.enums;

public enum BatchProcessStatus {
    ENABLED,
    DISABLED;

    public static BatchProcessStatus from(String value) {
        if (value == null) {
            return null;
        }
        try {
            return BatchProcessStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

