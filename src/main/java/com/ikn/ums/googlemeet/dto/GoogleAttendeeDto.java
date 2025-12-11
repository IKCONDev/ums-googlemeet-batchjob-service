package com.ikn.ums.googlemeet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GoogleAttendeeDto {

    @JsonProperty("email")
    private String email;

    @JsonProperty("responseStatus")
    private String responseStatus;   // accepted/declined/needsAction

    @JsonProperty("displayName")
    private String displayName;
}
