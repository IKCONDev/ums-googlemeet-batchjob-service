package com.ikn.ums.googlemeet.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "scheduled_meeting_attendee_rawdata_tab")
public class GoogleScheduledMeetingAttendee {

    @Id
    @SequenceGenerator(
            name = "google_scheduled_meeting_attendee_gen",
            sequenceName = "google_scheduled_meeting_attendee_seq",
            allocationSize = 1
    )
    @GeneratedValue(generator = "google_scheduled_meeting_attendee_gen")
    private Long sid;

    private String email;

    private Boolean organizer;    

    private Boolean self;         

    private String responseStatus; 

    @ManyToOne
    @JoinColumn(name = "sch_meeting_id", nullable = false)
    private GoogleScheduledMeeting meeting;
    
    private String id;
}
