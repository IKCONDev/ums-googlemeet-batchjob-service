package com.ikn.ums.googlemeet.service;

import java.util.List;

import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;

public interface GoogleCompletedMeetingService {

    /**
     * Fetches all Google Meet events (completed and/or scheduled)
     * from users’ calendars and performs batch processing,
     * such as mapping to DTOs and saving to the database.
     *
     * @return list of Google Meet event DTOs
     */
    List<GoogleCompletedMeetingDto> performMeetingsRawDataBatchProcessing();
}
