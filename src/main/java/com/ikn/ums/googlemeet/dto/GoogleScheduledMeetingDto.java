package com.ikn.ums.googlemeet.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleScheduledMeetingDto {
  
	@JsonProperty("id")
    private String eventid;                   
    private String recurringEventId;     
    private String summary;              
    private String description;          
    private String hangoutLink;              
    private String location;             
    private String organizerEmail;       
    private String created;            
    private String timezone;             
    //private EventTime start;             
    //private EventTime end;  
    //private String googleEventId;        
//
//    @Data
//    @AllArgsConstructor
//    @NoArgsConstructor
//    public static class EventTime {
//        private String dateTime;  
//        private String timeZone;  
//    }
//
//    public String getStartTime() {
//        return start != null ? start.getDateTime() : null;
//    }
//
//    public String getEndTime() {
//        return end != null ? end.getDateTime() : null;
//    }
//
//    public String getStartTimeZone() {
//        return start != null ? start.getTimeZone() : timezone;
//    }
//
//    public String getEndTimeZone() {
//        return end != null ? end.getTimeZone() : timezone;
//    }
    
    private String emailId;
    private Long departmentId;
    private Long teamId;
    private Long batchId;
    private String departmentName;
    private String teamName;
    
    private String meetingType;
    private List<String> recurrence;
    
    private String insertedBy = "AUTO-BATCH-PROCESS";
    private String insertedDate = LocalDateTime.now().toString();
    
    private List<GoogleScheduledMeetingAttendeeDto> attendees;
    
    private Long dbid;
    
    private StartDto start;
    private EndDto end;
    
    public String getStartTime() {
        return start != null ? start.getDateTime() : null;
    }

    public String getEndTime() {
        return end != null ? end.getDateTime() : null;
    }

    public String getStartTimeZone() {
        return start != null && start.getTimeZone() != null
                ? start.getTimeZone()
                : timezone;
    }

    public String getEndTimeZone() {
        return end != null && end.getTimeZone() != null
                ? end.getTimeZone()
                : timezone;
    }

    
    
    

}


