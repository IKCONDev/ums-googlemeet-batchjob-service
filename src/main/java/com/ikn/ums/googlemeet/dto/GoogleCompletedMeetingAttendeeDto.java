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
    private Boolean organizer;   

	@JsonProperty("self")
    private Boolean self;        

    @JsonProperty("responseStatus")
    private String responseStatus;

    //private GoogleCompletedMeetingDto meeting; 
}
