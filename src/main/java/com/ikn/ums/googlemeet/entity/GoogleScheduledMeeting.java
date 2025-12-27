package com.ikn.ums.googlemeet.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ikn.ums.googlemeet.enums.GoogleMeetingType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
    private Long dbid;

    //@Column(name = "google_event_id", unique = true, nullable = false)
    //private String googleEventId;       
    private String summary;           
    private String description;
    private String hostEmail;
    private String hostName;

    private String startTime;
    private String endTime;
   // private String duration;
    private String timezone;
    private String created;

    @Column(name = "hangoutLink")
    private String hangoutLink;
          // Hangout link -> event.getHangoutLink()


    private String insertedBy = "AUTO-BATCH-PROCESS";
    private String insertedDate = LocalDateTime.now().toString();

    private String emailId;
    private Long departmentId;
    private Long teamId;
    private Long batchId;
    private String departmentName;
    private String teamName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type")
    private GoogleMeetingType meetingType;
    
    
    @Column(name = "location")
    private String location;
    
    
    

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoogleScheduledMeetingAttendee> attendees = new ArrayList<>();
    public void addAttendee(GoogleScheduledMeetingAttendee attendee) {
        attendees.add(attendee);
        attendee.setMeeting(this);
    }
    
    private String recurringEventId; 
    
    private String eventid;

}
