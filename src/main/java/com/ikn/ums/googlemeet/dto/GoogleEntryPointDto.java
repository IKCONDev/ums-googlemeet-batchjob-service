package com.ikn.ums.googlemeet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleEntryPointDto {
    private String entryPointType; 
    private String uri;
    private String label;
    private String pin;          
    private String regionCode;   
}
