package com.ikn.ums.googlemeet.service;

import com.ikn.ums.googlemeet.entity.SchedulerConfig;

public interface GoogleSchedulerConfigService {

    /**
     * Fetches the scheduler configuration for upcoming/scheduled Google Meet events.
     * @return SchedulerConfig object, or null if not configured
     */
    SchedulerConfig getSchedulerConfigurationForScheduledMeetings();

    /**
     * Fetches the scheduler configuration for completed/past Google Meet events.
     * @return SchedulerConfig object, or null if not configured
     */
    SchedulerConfig getSchedulerConfigurationForPastMeetings();

    /**
     * Updates or saves the scheduler configuration for Google Meet events.
     * @param config SchedulerConfig object to save
     * @return Saved SchedulerConfig object
     */
    SchedulerConfig updateSchedulerConfig(SchedulerConfig config);
}
