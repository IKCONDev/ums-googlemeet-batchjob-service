package com.ikn.ums.googlemeet.dto;

import java.util.Collections;
import java.util.List;

public class UserExecutionResult<R> {

    private final boolean success;
    private final List<R> data;
    private final String failureReason;

    private UserExecutionResult(
            boolean success,
            List<R> data,
            String failureReason) {
        this.success = success;
        this.data = data;
        this.failureReason = failureReason;
    }

    public static <R> UserExecutionResult<R> success(List<R> data) {
        return new UserExecutionResult<>(true, data, null);
    }

    public static <R> UserExecutionResult<R> failure(String reason) {
        return new UserExecutionResult<>(false, Collections.emptyList(), reason);
    }

    public boolean isSuccess() {
        return success;
    }

    public List<R> getData() {
        return data;
    }

    public String getFailureReason() {
        return failureReason;
    }
}

