package com.ikn.ums.googlemeet.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

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
     * Executes async operations in batches and aggregates results.
     *
     * @param batches           partitioned input
     * @param asyncExecutor     async function (e.g. email -> CompletableFuture<List<R>>)
     * @param context           log context name
     */
    protected <T, R> List<R> executeInBatches(
            List<List<T>> batches,
            Function<T, CompletableFuture<List<R>>> asyncExecutor,
            String context) {

        List<R> masterResult = new ArrayList<>();
        int batchNumber = 1;

        for (List<T> batch : batches) {

            log.info("{} - Processing batch {} with {} item(s)",
                    context, batchNumber, batch.size());

            List<CompletableFuture<List<R>>> futures = new ArrayList<>();

            for (T item : batch) {
                futures.add(asyncExecutor.apply(item));
            }

            CompletableFuture
                    .allOf(futures.toArray(new CompletableFuture[0]))
                    .join();

            for (CompletableFuture<List<R>> future : futures) {
                try {
                    List<R> result = future.get();
                    if (result != null && !result.isEmpty()) {
                        masterResult.addAll(result);
                    }
                } catch (Exception ex) {
                    log.error("{} - Batch {} execution failed", context, batchNumber, ex);
                }
            }

            log.info("{} - Completed batch {} -> totalRecordsSoFar={}",
                    context, batchNumber, masterResult.size());

            batchNumber++;
        }

        return masterResult;
    }
}
