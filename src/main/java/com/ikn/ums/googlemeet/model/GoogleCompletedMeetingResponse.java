package com.ikn.ums.googlemeet.model;

import java.util.List;

import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleCompletedMeetingResponse {

    private String kind;                   
    private String etag;                
    private String summary;                
    private String timeZone;            
    private Integer pageNumber;         
    private Integer pageCount;         
    private Integer pageSize;              
    private Integer totalRecords;       
    private List<GoogleCompletedMeetingDto> meetings;  

}
