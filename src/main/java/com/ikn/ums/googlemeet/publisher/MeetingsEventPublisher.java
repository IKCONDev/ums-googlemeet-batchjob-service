package com.ikn.ums.googlemeet.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ikn.ums.googlemeet.events.CompletedMeetingsBatchEvent;
import com.ikn.ums.googlemeet.events.ScheduledMeetingsBatchEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingsEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.scheduled-meetings}")
    private String scheduledExchangeName;

    @Value("${rabbitmq.routing-key.scheduled-meetings}")
    private String scheduledRoutingKey;

    @Value("${rabbitmq.exchange.completed-meetings}")
    private String completedExchangeName;

    @Value("${rabbitmq.routing-key.completed-meetings}")
    private String completedRoutingKey;

    /**
     * Publishes a batch of scheduled meetings to RabbitMQ.
     */
    public void publishScheduledMeetingsEvent(ScheduledMeetingsBatchEvent batchEvent) {

        log.info("Publishing ScheduledMeetingsEvent - batchId={}, meetingCount={}",
                batchEvent.getBatchId(),
                batchEvent.getMeetings() != null ? batchEvent.getMeetings().size() : 0);

        try {
            rabbitTemplate.convertAndSend(scheduledExchangeName, scheduledRoutingKey, batchEvent);

            log.info("Successfully published ScheduledMeetingsEvent - batchId={} to exchange={}",
                    batchEvent.getBatchId(),
                    scheduledExchangeName);

        } catch (Exception e) {
            log.error("Failed to publish ScheduledMeetingsEvent - batchId={} | Reason={}",
                    batchEvent.getBatchId(),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    /**
     * Publishes a batch of completed meetings to RabbitMQ.
     */
    public void publishCompletedMeetingsEvent(CompletedMeetingsBatchEvent batchEvent) {

        log.info("Publishing CompletedMeetingsEvent - batchId={}, meetingCount={}",
                batchEvent.getBatchId(),
                batchEvent.getMeetings() != null ? batchEvent.getMeetings().size() : 0);

        try {
            rabbitTemplate.convertAndSend(completedExchangeName, completedRoutingKey, batchEvent);

            log.info("Successfully published CompletedMeetingsEvent - batchId={} to exchange={}",
                    batchEvent.getBatchId(),
                    completedExchangeName);

        } catch (Exception e) {
            log.error("Failed to publish CompletedMeetingsEvent - batchId={} | Reason={}",
                    batchEvent.getBatchId(),
                    e.getMessage(),
                    e);
            throw e;
        }
    }
}
