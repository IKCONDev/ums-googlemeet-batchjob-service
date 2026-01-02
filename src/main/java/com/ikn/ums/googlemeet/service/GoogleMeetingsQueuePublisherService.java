package com.ikn.ums.googlemeet.service;

import java.util.List;

import com.ikn.ums.googlemeet.externaldto.UMSCompletedMeetingDto;
import com.ikn.ums.googlemeet.externaldto.UMSScheduledMeetingDto;

public interface GoogleMeetingsQueuePublisherService {
	
	void publishScheduledMeetingsBatchEventInQueue(List<UMSScheduledMeetingDto> meetings, Long batchId);
	   
	void publishCompletedMeetingsBatchEventInQueue(List<UMSCompletedMeetingDto> meetings, Long batchId);

}
