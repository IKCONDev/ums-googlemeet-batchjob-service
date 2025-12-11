package com.ikn.ums.googlemeet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a Google Meet event for UMS batch processing.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleCompletedMeetingDto {

    private String id;                   
    private String recurringEventId;     //if recurringevent id == null single instance. , not null== recuuring instance.
    private String summary;              
    private String description;          
    private String hangoutLink;              
    private String location;             
    private String organizerEmail;       
    private String createdAt;            
    private String timezone;             
    private EventTime start;             
    private EventTime end;  
    private String googleEventId;		

    /**
     * Nested class for start/end times
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EventTime {
        private String dateTime;  // ISO-8601 string, e.g., "2025-12-09T10:00:00Z"
        private String timeZone;  
    }

    /**
     * Helper method to get start time as ISO string
     */
    public String getStartTime() {
        return start != null ? start.getDateTime() : null;
    }

    /**
     * Helper method to get end time as ISO string
     */
    public String getEndTime() {
        return end != null ? end.getDateTime() : null;
    }

    /**
     * Helper method to get start timezone
     */
    public String getStartTimeZone() {
        return start != null ? start.getTimeZone() : timezone;
    }

    /**
     * Helper method to get end timezone
     */
    public String getEndTimeZone() {
        return end != null ? end.getTimeZone() : timezone;
    }
    
    
    private String emailId;
    private Long departmentId;
    private Long teamId;
    private Long batchId;
    private String departmentName;
    private String teamName;
    
    
    
    
    private String MeetingType;

}





