package com.ikn.ums.googlemeet.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;

public interface GoogleAsyncService {

    /**
     * Fetches scheduled Google Meet meetings for a given user asynchronously.
     *
     * @param userId the email or userId of the employee
     * @return CompletableFuture containing the list of scheduled meetings
     */
    CompletableFuture<List<GoogleScheduledMeetingDto>> getScheduledMeetingsAsync(String userId);

    /**
     * Fetches completed Google Meet meetings for a given user asynchronously.
     *
     * @param userId the email or userId of the employee
     * @return CompletableFuture containing the list of completed meetings
     */
    CompletableFuture<List<GoogleCompletedMeetingDto>> getCompletedMeetingsAsync(String userId);
}
