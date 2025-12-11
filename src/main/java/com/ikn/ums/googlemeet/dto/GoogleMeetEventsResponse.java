package com.ikn.ums.googlemeet.dto;

import java.util.List;
import lombok.Data;

@Data
public class GoogleMeetEventsResponse {
    private List<GoogleCompletedMeetingDto> items;
}
