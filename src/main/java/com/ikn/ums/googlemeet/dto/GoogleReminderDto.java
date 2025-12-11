package com.ikn.ums.googlemeet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GoogleReminderDto {

    @JsonProperty("useDefault")
    private Boolean useDefault;
}
