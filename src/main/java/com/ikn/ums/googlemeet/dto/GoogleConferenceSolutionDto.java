package com.ikn.ums.googlemeet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleConferenceSolutionDto {
    private GoogleKeyDto key;
    private String name;
    private String iconUri;
}
