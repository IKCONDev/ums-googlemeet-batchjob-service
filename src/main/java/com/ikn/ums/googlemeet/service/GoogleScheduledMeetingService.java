package com.ikn.ums.googlemeet.service;

import java.util.List;

import org.springframework.stereotype.Service;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;

@Service
public interface GoogleScheduledMeetingService {
    
    List<GoogleScheduledMeetingDto> performScheduledMeetingsRawDataBatchProcessing();
}
