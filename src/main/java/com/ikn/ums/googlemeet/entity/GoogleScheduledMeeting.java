package com.ikn.ums.googlemeet.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "google_scheduled_meeting_rawdata_tab")
public class GoogleScheduledMeeting {

    @Id
    @SequenceGenerator(
            name = "google_scheduled_meeting_gen",
            sequenceName = "google_scheduled_meeting_seq",
            allocationSize = 1
    )
    @GeneratedValue(generator = "google_scheduled_meeting_gen")
    private Long id;

    private String googleEventId;      
    private String summary;           
    private String description;
    private String organizerEmail;
    private String meetingType;        

    private String startTime;
    private String endTime;
    private String duration;
    private String timezone;
    private String createdAt;

    private String joinUrl;            // Hangout link -> event.getHangoutLink()

   // @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<GoogleScheduledMeetingAttendee> attendees = new ArrayList<>();

    private String insertedBy = "AUTO-BATCH-PROCESS";
    private String insertedDate = LocalDateTime.now().toString();

    private String emailId;
    private Long departmentId;
    private Long teamId;
    private Long batchId;
    private String departmentName;
    private String teamName;
}
