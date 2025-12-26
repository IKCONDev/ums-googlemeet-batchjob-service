package com.ikn.ums.googlemeet.scheduler;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import com.ikn.ums.googlemeet.controller.GoogleMeetSourceDataBatchProcessController;
import com.ikn.ums.googlemeet.entity.SchedulerConfig;
import com.ikn.ums.googlemeet.service.GoogleSchedulerConfigService;

import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler for processing completed Google Meet events in batch.
 * <p>
 * It fetches the cron expression from the database using {@link GoogleMeetSchedulerConfigService}.
 * If no configuration is found, a fallback cron expression is used.
 * </p>
 * <p>
 * It triggers the batch-processing API exposed by
 * {@link GoogleMeetSourceDataBatchProcessController} and logs execution time and results.
 * </p>
 */
@Slf4j
@Component
public class GoogleCompletedMeetingsBatchScheduler implements SchedulingConfigurer {

    @Autowired
    private GoogleMeetSourceDataBatchProcessController googleMeetBatchController;

    @Autowired
    private GoogleSchedulerConfigService schedulerConfigService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

        String methodName = "configureTasks()";
        log.info("{} - Entered GoogleMeetCompletedMeetingsBatchScheduler", methodName);

        taskRegistrar.addTriggerTask(

            // Execute Task
            () -> {
                String methodNameInner = "runCompletedMeetingsBatch()";
                log.info("{} - Batch started at {}", methodNameInner, LocalDateTime.now());

                StopWatch stopwatch = new StopWatch();
                stopwatch.start();

                ResponseEntity<?> response =
                        googleMeetBatchController.performCompletedMeetingsRawDataBatchProcessing();

                stopwatch.stop();
                long ms = stopwatch.getTime();
                long timeTaken;
                String type;

                if (ms >= 60000) {
                    timeTaken = TimeUnit.MILLISECONDS.toMinutes(ms);
                    type = "minute(s)";
                } else if (ms >= 1000) {
                    timeTaken = TimeUnit.MILLISECONDS.toSeconds(ms);
                    type = "seconds";
                } else {
                    timeTaken = ms;
                    type = "milliseconds";
                }

                log.info("{} - Time taken: {} {}", methodNameInner, timeTaken, type);
                log.info("{} - Status: {} Response: {}", methodNameInner, response.getStatusCode(), response.getBody());
                log.info("{} - Batch completed at {}", methodNameInner, LocalDateTime.now());
            },

            // Trigger cron
            triggerContext -> {
                String methodNameInner = "cronTrigger()";

                SchedulerConfig config = schedulerConfigService.getSchedulerConfigurationForPastMeetings();

                String cron;
                if (config == null || config.getCronTime() == null || config.getCronTime().isBlank()) {
                    cron = "0 */1 * * * *"; // fallback cron (every 2 minutes)
                    log.info("{} - cronTime for completed Google Meet meetings not found in DB. Using fallback cron - {}", methodNameInner, cron);
                } else {
                    cron = config.getCronTime();
                    log.info("{} - Loaded cron from DB: {}", methodNameInner, cron);
                }

                return new CronTrigger(cron).nextExecution(triggerContext);
            }
        );
    }
}
