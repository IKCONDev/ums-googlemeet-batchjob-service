package com.ikn.ums.googlemeet.service.impl;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ikn.ums.googlemeet.events.CompletedMeetingsBatchEvent;
import com.ikn.ums.googlemeet.events.ScheduledMeetingsBatchEvent;
import com.ikn.ums.googlemeet.publisher.MeetingsEventPublisher;
import com.ikn.ums.googlemeet.externaldto.UMSCompletedMeetingDto;
import com.ikn.ums.googlemeet.externaldto.UMSScheduledMeetingDto;
import com.ikn.ums.googlemeet.service.GoogleMeetingsQueuePublisherService;

import lombok.extern.slf4j.Slf4j;

/**
 * Helper Service responsible for preparing and publishing scheduled and completed
 * Google meeting batch events into RabbitMQ.
 */
@Slf4j
@Service
public class GoogleMeetingsQueuePublisherServiceImpl implements GoogleMeetingsQueuePublisherService {

    @Autowired
    private MeetingsEventPublisher eventPublisher;

    /**
     * Prepares and publishes a batch event containing scheduled Google meetings.
     *
     * @param meetings list of scheduled meeting DTOs to publish
     */
    @Override
    public void publishScheduledMeetingsBatchEventInQueue(List<UMSScheduledMeetingDto> meetings) {

        ScheduledMeetingsBatchEvent event = new ScheduledMeetingsBatchEvent();
        event.setBatchId(Math.abs(new Random().nextLong()));
        event.setMeetings(meetings);
        event.setEventTimestamp(System.currentTimeMillis());

        log.info("Publishing scheduled meetings batch event: batchId={}, meetingCount={}",
                event.getBatchId(), meetings.size());

        eventPublisher.publishScheduledMeetingsEvent(event);
    }

    /**
     * Prepares and publishes a batch event containing completed Google meetings.
     *
     * @param meetings list of completed meeting DTOs to publish
     */
    @Override
    public void publishCompletedMeetingsBatchEventInQueue(List<UMSCompletedMeetingDto> meetings) {

        CompletedMeetingsBatchEvent event = new CompletedMeetingsBatchEvent();
        event.setBatchId(Math.abs(new Random().nextLong()));
        event.setMeetings(meetings);
        event.setEventTimestamp(System.currentTimeMillis());

        log.info("Publishing completed meetings batch event: batchId={}, meetingCount={}",
                event.getBatchId(), meetings.size());

        eventPublisher.publishCompletedMeetingsEvent(event);
    }
}
