package com.ikn.ums.googlemeet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "google_completed_meeting_rawdata_tab")
public class GoogleCompletedMeeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Primary key

    @Column(name = "google_event_id", unique = true, nullable = false)
    private String googleEventId; // Google event ID

    @Column(name = "summary")
    private String summary;

    @Column(name = "description")
    private String description;

    @Column(name = "start_time")
    private String startTime; // Store as string (ISO date-time)

    @Column(name = "end_time")
    private String endTime; // Store as string (ISO date-time)

    @Column(name = "hangoutLink")
    private String hangoutLink;
}
