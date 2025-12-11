package com.ikn.ums.googlemeet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Google Meeting Details DTO
 * Mirrors ZoomMeetingDetailsDto structure as closely as possible.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleMeetingDetailsDto {

    @JsonProperty("id")
    private String id;                     

    @JsonProperty("summary")
    private String topic;                  

    @JsonProperty("description")
    private String agenda;                 

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

    @JsonProperty("attendees")
    private List<GoogleAttendeeDto> attendees;  

    @JsonProperty("hangoutLink")
    private String joinUrl;                

    @JsonProperty("conferenceData")
    private GoogleConferenceDataDto conferenceData; 

    @JsonProperty("created")
    private String createdAt;

    @JsonProperty("updated")
    private String updatedAt;

    @JsonProperty("reminders")
    private GoogleReminderDto reminders;
}
