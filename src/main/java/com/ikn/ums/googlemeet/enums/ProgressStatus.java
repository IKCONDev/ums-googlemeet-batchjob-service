package com.ikn.ums.googlemeet.enums;

public enum ProgressStatus {

    SUCCESS,          // 100% successful
    PARTIAL_SUCCESS,  // completed with some failures
    FAILED,           // failed execution
    IN_PROGRESS;      // currently running

    public boolean isSuccessful() {
        return this == SUCCESS || this == PARTIAL_SUCCESS;
    }

    public boolean isFailure() {
        return this == FAILED;
    }

    public boolean isFinalState() {
        return this != IN_PROGRESS;
    }
}


