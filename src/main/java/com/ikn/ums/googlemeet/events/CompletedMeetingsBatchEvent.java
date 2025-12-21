package com.ikn.ums.googlemeet.events;

import java.util.List;

import com.ikn.ums.googlemeet.externaldto.UMSCompletedMeetingDto;

import lombok.Data;

/**
 * Event representing a batch of completed Google meet meetings pushed to the queue.
 * 
 */
@Data
public class CompletedMeetingsBatchEvent {

    private Long batchId;
    private List<UMSCompletedMeetingDto> meetings;
    private long eventTimestamp;
}
