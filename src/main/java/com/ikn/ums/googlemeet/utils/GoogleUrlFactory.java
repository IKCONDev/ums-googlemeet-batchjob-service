package com.ikn.ums.googlemeet.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class GoogleUrlFactory {

    @Value("${google.meetings.upcoming.url}")
    private String upcomingMeetingsUrl; // e.g., /calendar/v3/calendars/{calendarId}/events?timeMin={timeMin}&timeMax={timeMax}

    @Value("${google.meetings.completed.url}")
    private String completedMeetingsUrl; // e.g., /calendar/v3/calendars/{calendarId}/events

    @Value("${google.meetings.meeting-details.url}")
    private String meetingDetailsUrl; // /calendar/v3/calendars/{calendarId}/events/{eventId}

    @Value("${google.reports.attendance.url}")
    private String attendanceReportUrl; // /admin/reports/v1/activities?userKey={userKey}&applicationName=meet

    private static final String CALENDAR_BASE_URL = "https://www.googleapis.com/calendar/v3/calendars";

    // ---------------------------------------------
    // Upcoming Meetings URL
    // ---------------------------------------------
    
    public String buildUpcomingMeetingsUrl(String calendarId) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime sevenDaysLater = now.plusDays(7);

        String timeMin = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String timeMax = sevenDaysLater.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        return buildUpcomingMeetingsUrl(calendarId, timeMin, timeMax);
    }


    public String buildUpcomingMeetingsUrl(String calendarId, String timeMin, String timeMax) {
        Map<String, String> values = Map.of(
                "calendarId", calendarId,
                "timeMin", timeMin,
                "timeMax", timeMax
        );
        return UrlBuilderUtil.buildUrl(upcomingMeetingsUrl, values);
    }

    // ---------------------------------------------
    // Meeting Details URL
    // ---------------------------------------------
    public String buildMeetingDetailsUrl(String calendarId, String eventId) {
        Map<String, String> values = Map.of(
                "calendarId", calendarId,
                "eventId", eventId
        );
        return UrlBuilderUtil.buildUrl(meetingDetailsUrl, values);
    }

    // ---------------------------------------------
    // Completed Meetings URL
    // ---------------------------------------------
    public String buildCompletedMeetingsUrl(String calendarId, LocalDate fromDate) {
        String timeMin = fromDate.toString();
        String timeMax = LocalDate.now().toString();
        return CALENDAR_BASE_URL + "/" + calendarId + "/events?timeMin=" + timeMin +
                "&timeMax=" + timeMax + "&singleEvents=true&orderBy=startTime";
    }

    // ---------------------------------------------
    // Recurring Meeting Instances URL
    // ---------------------------------------------
    // Original method requiring calendarId
    public String buildRecurringDetailsUrl(String calendarId, String masterEventId) {
        return CALENDAR_BASE_URL + "/" + calendarId + "/events/" + masterEventId + "/instances";
    }

    // Overloaded method using "primary" as default calendarId
    public String buildRecurringDetailsUrl(String masterEventId) {
        return buildRecurringDetailsUrl("primary", masterEventId);
    }

    // ---------------------------------------------
    // Attendance Report URL
    // ---------------------------------------------
    public String buildAttendanceReportUrl(String userKey, String startTime, String endTime) {
        Map<String, String> values = new HashMap<>();
        values.put("userKey", userKey);
        values.put("startTime", startTime);
        values.put("endTime", endTime);
        return UrlBuilderUtil.buildUrl(attendanceReportUrl, values);
    }
}
