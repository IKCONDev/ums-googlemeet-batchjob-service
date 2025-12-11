package com.ikn.ums.googlemeet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GoogleConferenceDataDto {

    @JsonProperty("entryPoints")
    private List<GoogleEntryPointDto> entryPoints;  // dial-in, meet link

    @JsonProperty("conferenceId")
    private String conferenceId;

    @JsonProperty("signature")
    private String signature;
}
