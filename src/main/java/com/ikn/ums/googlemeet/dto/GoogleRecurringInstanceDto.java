package com.ikn.ums.googlemeet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleRecurringInstanceDto {

    @JsonProperty("id")
    private String instanceEventId;  
    
    private String recurringEventId; // recurring event instance ID

    @JsonProperty("start")
    private GoogleDateTimeDto start;

    @JsonProperty("end")
    private GoogleDateTimeDto end;

    @JsonProperty("status")
    private String status;

}
