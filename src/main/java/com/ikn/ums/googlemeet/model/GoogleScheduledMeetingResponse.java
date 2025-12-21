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
        
    private List<GoogleScheduledMeetingDto> items; 

}
