package com.ikn.ums.googlemeet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class GoogleRecurringMeetingDetailsDto {

    // Calendar Event Primary Fields
    @JsonProperty("id")
    private String eventId;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("description")
    private String description;

    @JsonProperty("status")
    private String status;

    @JsonProperty("created")
    private String created;

    @JsonProperty("updated")
    private String updated;

    @JsonProperty("start")
    private GoogleDateTimeDto start;

    @JsonProperty("end")
    private GoogleDateTimeDto end;

    @JsonProperty("timezone")
    private String timezone;

    // Recurrence Rule (RRULE)
    @JsonProperty("recurrence")
    private List<String> recurrence;

    @JsonProperty("entryPoints")
    private List<GoogleEntryPointDto> entryPoints;

    // Instances (Occurrences of the recurring meeting)
    @JsonProperty("instances")
    private List<GoogleRecurringInstanceDto> instances;
}
