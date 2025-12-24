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

    /**
     * conferenceRecords/.../transcripts/{uuid}
     */
    @Column(nullable = false, length = 512)
    private String transcriptName;

    /**
     * Google Docs fileId
     */
    @Column(nullable = false, length = 128)
    private String documentId;

    /**
     * Plain text exported via Drive API
     */
    
    @Column(columnDefinition = "TEXT")  // <-- Use TEXT for long content
    private String plainText;


    /**
     * Audit field
     */
    private OffsetDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private GoogleCompletedMeeting meeting;
    
    private String id;
}
