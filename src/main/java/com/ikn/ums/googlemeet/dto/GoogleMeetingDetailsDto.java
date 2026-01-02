package com.ikn.ums.googlemeet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Maps a Google Calendar / Meet event details.
 * Can be used for scheduled or completed meetings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleMeetingDetailsDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("recurringEventId")
    private String recurringEventId; // null if single event

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("description")
    private String description;

    @JsonProperty("location")
    private String location;

    @JsonProperty("status")
    private String status;

    @JsonProperty("start")
    private GoogleMeetingTimeDto start;

    @JsonProperty("end")
    private GoogleMeetingTimeDto end;

    @JsonProperty("creator")
    private GooglePersonDto creator;

    @JsonProperty("organizer")
    private GooglePersonDto organizer;

    @JsonProperty("hangoutLink")
    private String hangoutLink;

    @JsonProperty("htmlLink")
    private String htmlLink; 

    @JsonProperty("iCalUID")
    private String iCalUID; 

    @JsonProperty("conferenceData")
    private GoogleConferenceDataDto conferenceData;

    @JsonProperty("created")
    private String createdAt;

    @JsonProperty("updated")
    private String updatedAt;

    @JsonProperty("reminders")
    private GoogleReminderDto reminders;

    @JsonProperty("attendees")
    private List<GoogleCompletedMeetingAttendeeDto> attendees;

    
}
