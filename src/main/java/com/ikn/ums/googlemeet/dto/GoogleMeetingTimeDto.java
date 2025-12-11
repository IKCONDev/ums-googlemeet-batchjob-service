package com.ikn.ums.googlemeet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GoogleMeetingTimeDto {

    @JsonProperty("dateTime")
    private String dateTime;

    @JsonProperty("timeZone")
    private String timeZone;
}
