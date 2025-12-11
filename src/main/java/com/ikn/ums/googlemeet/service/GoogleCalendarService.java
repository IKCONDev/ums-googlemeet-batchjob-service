package com.ikn.ums.googlemeet.service;


import java.util.List;

import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
//import com.ikn.ums.googlemeet.dto.GoogleAttendeeDto;
import com.ikn.ums.googlemeet.model.AccessTokenResponseModel;

/**
 * GoogleEventService
 *
 * Provides an abstraction layer for all outbound calls made to
 * Google Calendar / Google Meet APIs. Implementations of this interface must handle:
 *
 * <ul>
 *     <li>Access token retrieval</li>
 *     <li>Fetching scheduled/upcoming meetings</li>
 *     <li>Fetching completed meetings</li>
 *     <li>Fetching meeting invitees and metadata</li>
 *     <li>Error handling, retry logic, and fail-safe returns</li>
 * </ul>
 *
 * All methods must guarantee safe failure by returning empty lists instead
 * of throwing exceptions to the service layer.
 */
public interface GoogleCalendarService {

    /**
     * Retrieves the OAuth access token required for making
     * authorized calls to Google APIs.
     *
     * @return the active Google access token, or null if loading failed
     */
    AccessTokenResponseModel getAccessToken();

    /**
     * Fetches the list of upcoming/scheduled Google Meet events
     * for the provided user.
     *
     * Implementations should include:
     * <ul>
     *     <li>Access token validation</li>
     *     <li>Retry logic for network issues</li>
     *     <li>Handling of Google API rate limits</li>
     * </ul>
     *
     * @param userEmail Google user email
     * @return list of scheduled meeting DTOs (never null)
     */
    List<GoogleScheduledMeetingDto> fetchScheduledMeetings(String userEmail);

    /**
     * Fetches the invitee list for a specific Google Meet event.
     *
     * @param eventId the Google Calendar event ID
     * @return list of attendees (never null)
     */
   // List<GoogleAttendeeDto> fetchAttendees(String eventId);

    /**
     * Fetches the list of completed Google Meet events
     * for the provided user.
     *
     * Implementations should apply:
     * <ul>
     *     <li>Access token handling</li>
     *     <li>Retry logic for network failures</li>
     *     <li>Rate-limit handling</li>
     * </ul>
     *
     * @param userEmail Google user email
     * @return list of completed meeting DTOs (never null)
     */
    List<GoogleCompletedMeetingDto> fetchCompletedMeetings(String userEmail);

    /**
     * Fetches the child instances of a recurring Google Meet event.
     *
     * Google Calendar represents recurring events with a master event ID,
     * and individual occurrences can be retrieved separately.
     *
     * @param masterEventId The master event ID of the recurring meeting
     * @return list of child meeting instances, or empty list if none
     */
    List<GoogleScheduledMeetingDto> fetchRecurringInstances(String masterEventId);
}
