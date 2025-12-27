package com.ikn.ums.googlemeet.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ikn.ums.googlemeet.dto.GoogleRecurringInstanceDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class GoogleRecurringInstancesResponse {
	
	@JsonProperty("items")
    private List<GoogleRecurringInstanceDto> items;

}
