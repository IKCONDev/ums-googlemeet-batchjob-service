package com.ikn.ums.googlemeet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GoogleCompletedMeetingAttendeeDto {

	@JsonProperty("email")
    private String email;

	@JsonProperty("organizer")
    private Boolean organizer;   // optional, may be null if not present

	@JsonProperty("self")
    private Boolean self;        // optional, may be null if not present

    @JsonProperty("responseStatus")
    private String responseStatus;

    private GoogleCompletedMeetingDto meeting; // link to meeting if needed
}
