package com.ikn.ums.googlemeet.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ikn.ums.googlemeet.entity.SchedulerConfig;
import com.ikn.ums.googlemeet.repo.GoogleSchedulerConfigRepository;
import com.ikn.ums.googlemeet.service.GoogleSchedulerConfigService;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link GoogleSchedulerConfigService} that handles
 * retrieval and update of scheduler configurations for Google Meet batch jobs.
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Fetching scheduler configs for Scheduled and Past Google Meet meetings</li>
 *     <li>Building cron expressions dynamically</li>
 *     <li>Creating new records if none exist for a batch type</li>
 *     <li>Persisting updates to DB</li>
 * </ul>
 */
@Slf4j
@Service
public class GoogleSchedulerConfigServiceImpl implements GoogleSchedulerConfigService {

    @Autowired
    private GoogleSchedulerConfigRepository repo;

    @Override
    public SchedulerConfig getSchedulerConfigurationForScheduledMeetings() {
        String methodName = "getSchedulerConfigurationForScheduledMeetings()";
        log.info("{} - Fetching scheduler config for Scheduled Google Meet meetings", methodName);

        SchedulerConfig config = repo.findByTypeOfBatch("Scheduled");

        log.info("{} - Retrieved config: {}", methodName, config);
        return config;
    }

    @Override
    public SchedulerConfig getSchedulerConfigurationForPastMeetings() {
        String methodName = "getSchedulerConfigurationForPastMeetings()";
        log.info("{} - Fetching scheduler config for Past Google Meet meetings", methodName);

        SchedulerConfig config = repo.findByTypeOfBatch("Completed");

        log.info("{} - Retrieved config: {}", methodName, config);
        return config;
    }

    @Transactional
    @Override
    public SchedulerConfig updateSchedulerConfig(SchedulerConfig config) {

        String methodName = "updateSchedulerConfig()";
        log.info("{} - Entered with config: {}", methodName, config);

        // Fetch existing config
        SchedulerConfig dbConfig = repo.findByTypeOfBatch(config.getTypeOfBatch());

        if (dbConfig == null) {
            log.info("{} - No existing config found, creating new record", methodName);

            dbConfig = new SchedulerConfig();
            dbConfig.setCronId(1); // initial ID or sequence
            dbConfig.setTypeOfBatch(config.getTypeOfBatch());
        } else {
            log.info("{} - Found existing config: {}", methodName, dbConfig);
        }

        String hour = config.getHour();
        String minute = config.getMinute();

        log.info("{} - Hour received: {}, Minute received: {}", methodName, hour, minute);

        // Build cron expression
        String cronTime;

        if ("0".equals(hour)) {
            cronTime = "0 */" + minute + " * * * *";
            hour = "*";
            log.info("{} - Built cron for minutes only: {}", methodName, cronTime);
        } else {
            if ("0".equals(minute)) {
                cronTime = "0 0 */" + hour + " * * *";
                log.info("{} - Built cron for hours only: {}", methodName, cronTime);
            } else {
                cronTime = "0 */" + minute + " */" + hour + " * * *";
                log.info("{} - Built cron for hours and minutes: {}", methodName, cronTime);
            }
        }

        // Populate entity
        dbConfig.setCronTime(cronTime);
        dbConfig.setHour(hour);
        dbConfig.setMinute(minute);
        dbConfig.setModifiedBy(config.getModifiedBy());

        log.info("{} - Saving updated config: {}", methodName, dbConfig);

        SchedulerConfig updatedConfig = repo.save(dbConfig);

        log.info("{} - Final updated cronTime: {}", methodName, cronTime);

        return updatedConfig;
    }
}
