package com.ikn.ums.googlemeet.dto;

import java.time.LocalDateTime;
import java.util.List;



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
    private String recurringEventId;     
    private String summary;              
    private String description;          
    private String hangoutLink;              
    private String location;             
    private String organizerEmail;       
    private String createdAt;            
    private String timezone;             
    private EventTime start;             
    private EventTime end;  
    //private String googleEventId; 

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EventTime {
        private String dateTime;  // ISO-8601 string, e.g., "2025-12-09T10:00:00Z"
        private String timeZone;  
    }

    public String getStartTime() {
        return start != null ? start.getDateTime() : null;
    }

    public String getEndTime() {
        return end != null ? end.getDateTime() : null;
    }

    public String getStartTimeZone() {
        return start != null ? start.getTimeZone() : timezone;
    }

    public String getEndTimeZone() {
        return end != null ? end.getTimeZone() : timezone;
    }

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

    private List<GoogleCompletedMeetingAttendeeDto> attendees;
    
 private List<GoogleCompletedMeetingParticipantDto> participants;
    
    private List<TranscriptDto> transcripts;
    
    private String conferenceRecordId;
    
    // Drive API plain text
    private List<PlainTranscriptDto> plainTranscripts;
    
    private Long sid;
    
    
    
}
