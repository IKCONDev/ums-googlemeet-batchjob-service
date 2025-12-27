package com.ikn.ums.googlemeet.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ikn.ums.googlemeet.enums.GoogleMeetingType;


//import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "google_completed_meeting_rawdata_tab")
public class GoogleCompletedMeeting {

    @Id
    @SequenceGenerator(
            name = "google_completed_meeting_gen",
            sequenceName = "google_completed_meeting_seq",
            allocationSize = 1
    )
    @GeneratedValue(generator = "google_completed_meeting_gen")
    private Long dbid;

//    @Column(name = "google_event_id", unique = true, nullable = false)
//    private String googleEventId;       
    private String summary;           
    private String description;
    private String hostEmail;
    private String hostName;
    
    @Column(name = "location")
    private String location;
         

    private String startTime;
    private String endTime;
    //private String duration;
    private String timezone;
    private String created;

    @Column(name = "hangoutLink")
    private String hangoutLink;
          // Hangout link -> event.getHangoutLink()


    private String insertedBy = "AUTO-BATCH-PROCESS";
    private String insertedDate = LocalDateTime.now().toString();

//    private String emailId;
//    private Long departmentId;
//    private Long teamId;
//    private Long batchId;
//    private String departmentName;
//    private String teamName;
//    
    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type")
    private GoogleMeetingType meetingType;
    
    
    

//    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<GoogleCompletedMeetingAttendee> attendees = new ArrayList<>();
//    public void addAttendee(GoogleCompletedMeetingAttendee attendee) {
//        attendees.add(attendee);
//        attendee.setMeeting(this);
//    }
    
    private String recurringEventId; 
    
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoogleCompletedMeetingParticipant> participants = new ArrayList<>();
    
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoogleMeetTranscriptEntity> transcripts = new ArrayList<>();
    
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlainTranscriptEntity> plaintranscript = new ArrayList<>();
    
    @Column(name = "conference_record_id", unique = true)
    private String conferenceRecordId;
    
    
    
    private String eventid;
    
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<GoogleCompletedMeetingAttendee> attendees = new ArrayList<>();
    
    
    //---------------- TRANSIENT (NOT PERSISTED) ----------------
    @Transient
    private String emailId;
    @Transient
    private Long departmentId;
    @Transient
    private Long teamId;
    private Long batchId;
    @Transient
    private String departmentName;
    @Transient
    private String teamName;


}
