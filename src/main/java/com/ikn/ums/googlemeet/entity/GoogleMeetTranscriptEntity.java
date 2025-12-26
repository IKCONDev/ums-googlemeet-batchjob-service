package com.ikn.ums.googlemeet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "google_meeting_transcripts")
public class GoogleMeetTranscriptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dbid;

    private String name;
    private String state;

    @Column(name = "start_time")
    private String startTime;

    @Column(name = "end_time")
    private String endTime;
    
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "doscdestination")
    private DocsDestinationEntity docsDestination;

//    @Column(name = "document_id")
//    private String document;
//
//    @Column(name = "export_uri")
//    private String exportUri;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private GoogleCompletedMeeting meeting;
    
    private String eventid;
    
    
    @Column(columnDefinition = "TEXT")  // <-- Use TEXT for long content
    private String plainText;

}
