package com.ikn.ums.googlemeet.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.ikn.ums.googlemeet.dto.BatchExecutionResult;
import com.ikn.ums.googlemeet.dto.UserExecutionResult;
import com.ikn.ums.googlemeet.enums.ProgressStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractBatchExecutor {

    /**
     * Partitions a list into batches of given size.
     */
    protected <T> List<List<T>> partition(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();

        if (list == null || list.isEmpty() || batchSize <= 0) {
            return batches;
        }

        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }

        return batches;
    }

    /**
     * Executes async operations in batches and aggregates success/failure results.
     *
     * @param batches           partitioned input
     * @param asyncExecutorFunc async function returning UserExecutionResult<R>
     * @param context           log context
     */
    protected <T, R> BatchExecutionResult<T, R> executeInBatches(
            List<List<T>> batches,
            Function<T, CompletableFuture<UserExecutionResult<R>>> asyncExecutorFunc,
            String context) {

        BatchExecutionResult<T, R> result = new BatchExecutionResult<>();
        int batchNumber = 1;

        for (List<T> batch : batches) {

            log.info("{} - Processing batch {} with {} item(s)", context, batchNumber, batch.size());

            List<Map.Entry<T, CompletableFuture<UserExecutionResult<R>>>> tasks = new ArrayList<>();
            for (T item : batch) {
                tasks.add(Map.entry(item, asyncExecutorFunc.apply(item)));
            }

            CompletableFuture
                    .allOf(tasks.stream()
                            .map(Map.Entry::getValue)
                            .toArray(CompletableFuture[]::new))
                    .join();

            for (Map.Entry<T, CompletableFuture<UserExecutionResult<R>>> task : tasks) {
                try {
                    UserExecutionResult<R> execResult = task.getValue().get();

                    if (execResult == null) {
                        result.addFailedItem(task.getKey());
                        log.error("{} - Null execution result for item={}", context, task.getKey());
                        continue;
                    }

                    // Success even if the returned list is empty
                    if (execResult.isSuccess()) {
                        result.addSuccessItem(task.getKey());
                        if (execResult.getData() != null && !execResult.getData().isEmpty()) {
                            result.addSuccessResult(execResult.getData());
                        }
                    } else {
                        result.addFailedItem(task.getKey());
                        log.error("{} - Failed for item={} -> {}", context, task.getKey(), execResult.getFailureReason());
                    }

                } catch (Exception ex) {
                    result.addFailedItem(task.getKey());
                    log.error("{} - Unexpected failure for item={}", context, task.getKey(), ex);
                }
            }

            batchNumber++;
        }

        return result;
    }

    /**
     * Computes overall progress status of batch execution.
     */
    protected ProgressStatus calculateBatchStatus(BatchExecutionResult<?, ?> result) {
        int failed = result.getFailedItems().size();
        int success = result.getSuccessItems().size();

        if (failed == 0) {
            return ProgressStatus.SUCCESS;
        } else if (success > 0) {
            return ProgressStatus.PARTIAL_SUCCESS;
        }
        return ProgressStatus.FAILED;
    }

    /**
     * Extract emails from items using the provided extractor function.
     */
    protected <T> List<String> extractEmails(List<T> items, Function<T, String> emailExtractor) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        return items.stream()
                .map(emailExtractor)
                .filter(e -> e != null && !e.isBlank())
                .toList();
    }
}
