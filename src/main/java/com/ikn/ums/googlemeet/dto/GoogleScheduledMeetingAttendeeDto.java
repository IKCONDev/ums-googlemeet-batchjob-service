package com.ikn.ums.googlemeet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GoogleScheduledMeetingAttendeeDto {

    private Long dbid; 

    private String email;

    private Boolean organizer; // true if attendee is organizer, may be null

    private Boolean self;      // true if attendee is self, may be null

    @JsonProperty("responseStatus")
    private String responseStatus; // "accepted", "needsAction"
}
