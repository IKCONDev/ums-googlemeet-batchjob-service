package com.ikn.ums.googlemeet.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "google_meet_plain_transcript")
    


public class PlainTranscriptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sid;

    
    @Column(nullable = false, length = 512)
    private String transcriptName;

   
    @Column(nullable = false, length = 128)
    private String documentId;

    
    
    @Column(columnDefinition = "TEXT")  // <-- Use TEXT for long content
    private String plainText;


    private OffsetDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private GoogleCompletedMeeting meeting;
    
    private String id;
}
