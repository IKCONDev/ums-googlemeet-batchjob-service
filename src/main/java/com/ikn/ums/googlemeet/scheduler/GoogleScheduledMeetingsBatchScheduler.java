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
 * Scheduler for executing Google Meet scheduled meeting batch-processing task.
 * It dynamically reads cron expressions from DB and executes the batch processor.
 */
@Slf4j
@Component
public class GoogleScheduledMeetingsBatchScheduler implements SchedulingConfigurer {

    @Autowired
    private GoogleMeetSourceDataBatchProcessController googleMeetBatchProcessingController;

    @Autowired
    private GoogleSchedulerConfigService schedulerConfigService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

        String methodName = "configureTasks()";
        log.info("{} - Entered GoogleScheduledMeetingsBatchScheduler", methodName);

        taskRegistrar.addTriggerTask(

            // -------------------- EXECUTE TASK ------------------------
            () -> {
                String innerMethod = "runScheduledMeetingsBatch()";
                log.info("{} - Google Meet Batch started at {}", innerMethod, LocalDateTime.now());

                StopWatch stopwatch = new StopWatch();
                stopwatch.start();

                ResponseEntity<?> response =
                        googleMeetBatchProcessingController.performScheduledMeetingsRawDataBatchProcessing();

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

                log.info("{} - Time taken: {} {}", innerMethod, timeTaken, type);
                log.info("{} - Status: {} Response: {}", innerMethod, response.getStatusCode(), response.getBody());
                log.info("{} - Google Meet Batch completed at {}", innerMethod, LocalDateTime.now());
            },

            // -------------------- CRON TRIGGER FROM DB ------------------------
            triggerContext -> {

                String innerMethod = "cronTrigger()";

                SchedulerConfig config =
                        schedulerConfigService.getSchedulerConfigurationForScheduledMeetings();

                String cron;

                if (config == null) {
                    cron = "0 */1 * * * *"; // fallback every 1 minute
                    log.info("{} - SchedulerConfig NULL. Using fallback: {}", innerMethod, cron);
                    return new CronTrigger(cron).nextExecution(triggerContext);
                }

                cron = config.getCronTime();

                if (cron == null || cron.isBlank()) {
                    cron = "0 */1 * * * *";
                    log.info("{} - cronTime NULL/BLANK. Using fallback: {}", innerMethod, cron);
                } else {
                    log.info("{} - Loaded Google Meet cron from DB: {}", innerMethod, cron);
                }

                return new CronTrigger(cron).nextExecution(triggerContext);
            }
        );
    }
}
