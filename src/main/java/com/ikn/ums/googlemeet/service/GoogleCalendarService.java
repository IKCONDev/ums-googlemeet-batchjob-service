package com.ikn.ums.googlemeet.service;

import java.util.List;

import com.ikn.ums.googlemeet.dto.ConferenceRecordDto;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingParticipantDto;
import com.ikn.ums.googlemeet.dto.GoogleMeetingDetailsDto;
import com.ikn.ums.googlemeet.dto.GoogleRecurringMeetingDetailsDto;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.dto.TranscriptDto;
import com.ikn.ums.googlemeet.model.AccessTokenResponseModel;

/**
 * GoogleCalendarService
 *
 * Acts as the primary abstraction over Google Calendar APIs
 * for Google Meet-related scheduling and metadata.
 *
 * IMPORTANT:
 *  - Calendar API → meetings, events, invitees, recurrence
 *  - Meet API → participants, attendance, conference records
 *
 * This interface intentionally exposes BOTH because
 * Google splits data across two APIs.
 */
public interface GoogleCalendarService {

    /**
     * Retrieves the OAuth access token used for Google API calls.
     *
     * @return access token response model
     */
    AccessTokenResponseModel getAccessToken();

    /**
     * Fetch upcoming (scheduled) Google Meet meetings.
     *
     * @param userEmail Google Workspace user email
     * @return list of scheduled meetings (never null)
     */
    List<GoogleScheduledMeetingDto> fetchScheduledMeetings(String userEmail);

    /**
     * Fetch completed Google Meet meetings.
     *
     * NOTE:
     * These are Calendar EVENTS, not attendance records.
     *
     * @param userEmail Google Workspace user email
     * @return list of completed meetings (never null)
     */
    List<GoogleCompletedMeetingDto> fetchCompletedMeetings(String userEmail);

    /**
     * Fetch full Google Calendar event details.
     *
     * @param eventId Google Calendar event ID
     * @return meeting details or null
     */
    GoogleMeetingDetailsDto fetchMeetingDetails(String eventId);

    /**
     * Fetch calendar invitees (attendees) for a meeting.
     *
     * NOTE:
     * These are INVITEES, not actual participants.
     *
     * @param eventId Google Calendar event ID
     * @param attendeeType DTO class
     * @param <T> attendee DTO type
     * @return list of invitees (never null)
     */
    <T> List<T> fetchInvitees(String eventId, Class<T> attendeeType);

    /**
     * Fetch recurring meeting master event details.
     *
     * @param recurringEventId master event ID
     * @return recurring meeting details
     */
    GoogleRecurringMeetingDetailsDto fetchRecurringMeetingDetails(String recurringEventId);

    /**
     * Fetch instances (occurrences) of a recurring meeting.
     *
     * Uses:
     *   GET /events/{eventId}/instances
     *
     * @param masterEventId recurring master event ID
     * @return list of meeting instances
     */
    List<GoogleScheduledMeetingDto> fetchRecurringInstances(String masterEventId);
    
    
    

    /** Fetch participants of a conference record */
    List<GoogleCompletedMeetingParticipantDto> fetchParticipants(String conferenceRecordId);

    /** Fetch transcripts of a conference record */
    List<TranscriptDto> fetchTranscripts(String conferenceRecordId);

	List<ConferenceRecordDto> fetchConferenceRecords();


}
