package com.ikn.ums.googlemeet.events;

import java.util.List;

import com.ikn.ums.googlemeet.externaldto.UMSScheduledMeetingDto;

import lombok.Data;

/**
 * Event representing a batch of scheduled Google meet meetings pushed to the queue.
 * 
 */
@Data
public class ScheduledMeetingsBatchEvent {

    private Long batchId;
    private List<UMSScheduledMeetingDto> meetings;
    private long eventTimestamp;
}
