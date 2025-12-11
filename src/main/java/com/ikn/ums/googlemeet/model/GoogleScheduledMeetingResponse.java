package com.ikn.ums.googlemeet.model;


import java.util.List;

import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleScheduledMeetingResponse {

    private String kind;                   
    private String etag;                  
    private String summary;                
    private String timeZone;               
    private String nextSyncToken;          
    private Integer totalRecords;          
    private List<GoogleScheduledMeetingDto> meetings; 

}
