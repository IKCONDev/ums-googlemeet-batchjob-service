package com.ikn.ums.googlemeet.service;

import com.ikn.ums.googlemeet.dto.BatchExecutionDetailDto;

public interface BatchExecutionDetailService {

    /**
     * Creates a new batch execution entry with IN_PROGRESS status.
     */
    BatchExecutionDetailDto startBatch(String batchName);

    /**
     * Completes an existing batch execution and updates final status.
     */
    BatchExecutionDetailDto completeBatch(BatchExecutionDetailDto batchDetailDto);
}
