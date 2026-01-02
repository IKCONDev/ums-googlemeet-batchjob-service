package com.ikn.ums.googlemeet.dto;

import java.util.ArrayList;
import java.util.List;

public class BatchExecutionResult<T, R> {

    private final List<R> successResults = new ArrayList<>();
    private final List<T> failedItems = new ArrayList<>();
    private final List<T> successItems = new ArrayList<>();

    public void addSuccessResult(List<R> results) {
        if (results != null) {
            successResults.addAll(results);
        }
    }

    public void addSuccessItem(T item) {
        successItems.add(item);
    }

    public void addFailedItem(T item) {
        failedItems.add(item);
    }

    public List<R> getSuccessResults() {
        return successResults;
    }

    public List<T> getFailedItems() {
        return failedItems;
    }

    public List<T> getSuccessItems() {
        return successItems;
    }
}
