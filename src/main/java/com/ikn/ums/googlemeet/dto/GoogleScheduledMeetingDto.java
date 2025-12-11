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

    @JsonProperty("db_id")
    private Long id;

    @JsonProperty("id")
    private String googleEventId;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("description")
    private String description;

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("end_time")
    private String endTime;

    @JsonProperty("time_zone")
    private String timezone;

    @JsonProperty("join_url")
    private String joinUrl;

    @JsonProperty("created_at")
    private String createdAt;            

    private String recurringEventId;     

    private String location;             

    private String organizerEmail;       

    private List<String> attendees;

    private String insertedBy = "AUTO-BATCH-PROCESS";
    private String insertedDate = LocalDateTime.now().toString();

    private String emailId;
    private Long departmentId;
    private Long teamId;
    private Long batchId;
    private String departmentName;
    private String teamName;
    
    
    
    
    private String meetingType;
}
