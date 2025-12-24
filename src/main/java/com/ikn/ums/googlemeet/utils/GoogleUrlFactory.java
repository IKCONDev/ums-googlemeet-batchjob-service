package com.ikn.ums.googlemeet.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * GoogleUrlFactory
 *
 * Responsible for constructing Google Calendar & Google Meet API URLs
 * using placeholder-based templates defined in application properties.
 *
 * Design is intentionally aligned with ZoomUrlFactory for consistency.
 */
@Component
public class GoogleUrlFactory {

   

    @Value("${google.calendarBaseUrl}")
    private String calendarBaseUrl;   // https://www.googleapis.com/calendar/v3

    @Value("${google.meetBaseUrl}")
    private String meetBaseUrl;        // https://meet.googleapis.com/v2


    @Value("${google.meetings.upcoming.url}")
    private String upcomingMeetingsUrl;

    @Value("${google.meetings.completed.url}")
    private String completedMeetingsUrl;

    @Value("${google.meetings.meeting-details.url}")
    private String meetingDetailsUrl;

    @Value("${google.meetings.recurring-master.url}")
    private String recurringMasterUrl;

    @Value("${google.meetings.recurring-occurrences.url}")
    private String recurringOccurrencesUrl;

    @Value("${google.meetings.participants.url}")
    private String participantsUrl;

    @Value("${google.meetings.conference-records.url}")
    private String conferenceRecordsUrl;

    @Value("${google.meetings.transcripts.url}")
    private String transcriptsUrl;
    
    
    
    @Value("${google.driveBaseUrl}")
    private String driveBaseUrl;   // https://www.googleapis.com/drive/v3

    
    


    /**
     * Builds URL to fetch upcoming/scheduled meetings.
     *
     * Google Endpoint:
     *   GET /calendars/{calendarId}/events
     */
    public String buildUpcomingMeetingsUrl(String calendarId) {

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime sevenDaysLater = now.plusDays(7);

        Map<String, String> values = new HashMap<>();
        values.put("calendarId", calendarId);
        values.put("timeMin", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        values.put("timeMax", sevenDaysLater.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        return UrlBuilderUtil.buildUrl(upcomingMeetingsUrl, values);
    }



    /**
     * Builds URL to fetch completed meetings from a given date till now.
     *
     * Google Endpoint:
     *   GET /calendars/{calendarId}/events?timeMin=&timeMax=
     */
//    public String buildCompletedMeetingsUrl(String calendarId, LocalDate fromDate) {
//
//        String fromUtc = fromDate.atStartOfDay(ZoneOffset.UTC)
//                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
//
//        String toUtc = ZonedDateTime.now(ZoneOffset.UTC)
//                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
//
//        Map<String, String> values = new HashMap<>();
//        values.put("calendarId", calendarId);
//        values.put("fromUtc", fromUtc);
//        values.put("toUtc", toUtc);
//
//        return UrlBuilderUtil.buildUrl(completedMeetingsUrl, values);
//    }
    
    public String buildCompletedMeetingsUrl(String calendarId) {

        // Current UTC time
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        // Define past period, e.g., 7 days ago
        ZonedDateTime sevenDaysAgo = now.minusDays(7);

        Map<String, String> values = new HashMap<>();
        values.put("calendarId", calendarId);
        values.put("timeMin", sevenDaysAgo.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        values.put("timeMax", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        return UrlBuilderUtil.buildUrl(completedMeetingsUrl, values);
    }



    /**
     * Builds URL to fetch meeting/event details.
     *
     * Google Endpoint:
     *   GET /calendars/{calendarId}/events/{eventId}
     */
    public String buildMeetingDetailsUrl(String calendarId, String eventId) {
        Map<String, String> values = Map.of(
                "calendarId", calendarId,
                "eventId", eventId
        );
        return UrlBuilderUtil.buildUrl(meetingDetailsUrl, values);
    }

    /**
     * Builds URL to fetch recurring meeting master details.
     */
    public String buildRecurringDetailsUrl(String calendarId, String recurringEventId) {
        Map<String, String> values = Map.of(
                "calendarId", calendarId,
                "recurringEventId", recurringEventId
        );
        return UrlBuilderUtil.buildUrl(recurringMasterUrl, values);
    }

    /**
     * Builds URL to fetch recurring meeting occurrences.
     */
    public String buildRecurringOccurrencesUrl(String calendarId, String recurringEventId) {
        Map<String, String> values = Map.of(
                "calendarId", calendarId,
                "recurringEventId", recurringEventId
        );
        return UrlBuilderUtil.buildUrl(recurringOccurrencesUrl, values);
    }


    /**
     * Builds URL to fetch participants of a completed Google Meet.
     *
     * Google Meet Endpoint:
     *   GET /conferenceRecords/{conferenceRecordId}/participants
     */
//    public String buildParticipantsUrl(String conferenceRecordId) {
//        Map<String, String> values = Map.of(
//                "conferenceRecordId", encode(conferenceRecordId)
//        );
//        return UrlBuilderUtil.buildUrl(participantsUrl, values);
//    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
    public String buildMeetingsDetailsUrl(String eventId) {
        // Base URL for Google Calendar API
        String calendarId = "primary"; // or any specific calendar ID
        return "https://www.googleapis.com/calendar/v3/calendars/" + calendarId + "/events/" + eventId + "?conferenceDataVersion=1";
    }
    
    
    /**
     * Builds URL to list conference records.
     *
     * Google Meet Endpoint:
     *   GET /conferenceRecords
     */
    public String buildConferenceRecordsUrl() {
        return conferenceRecordsUrl;
    }

    /**
     * Builds URL to fetch participants of a completed Google Meet.
     */
    public String buildConferenceParticipantsUrl(String conferenceRecordId) {
        Map<String, String> values = Map.of(
                "conferenceRecordId",conferenceRecordId
        );
        return UrlBuilderUtil.buildUrl(participantsUrl, values);
    }

    /**
     * Builds URL to fetch transcripts of a completed Google Meet.
     */
    public String buildConferenceTranscriptsUrl(String conferenceRecordId) {
        Map<String, String> values = Map.of(
                "conferenceRecordId", conferenceRecordId
        );
        return UrlBuilderUtil.buildUrl(transcriptsUrl, values);
    }

    
    
    
    /**
     * Builds URL to export Google Docs transcript as plain text.
     *
     * Google Drive Endpoint:
     *   GET /drive/v3/files/{fileId}/export?mimeType=text/plain
     */
    public String buildPlainTranscriptExportUrl(String documentId) {

        return driveBaseUrl
                + "/files/"
                + documentId
                + "/export?mimeType=text/plain";
    }

    

}
