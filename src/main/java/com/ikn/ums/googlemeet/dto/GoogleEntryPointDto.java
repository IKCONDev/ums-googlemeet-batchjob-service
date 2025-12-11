package com.ikn.ums.googlemeet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GoogleEntryPointDto {

    @JsonProperty("entryPointType")
    private String entryPointType; // e.g., "video", "phone"

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("label")
    private String label;

    @JsonProperty("pin")
    private String pin;
}
