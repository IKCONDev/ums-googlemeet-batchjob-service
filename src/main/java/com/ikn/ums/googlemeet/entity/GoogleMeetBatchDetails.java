package com.ikn.ums.googlemeet.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "googlemeet_batch_tab")
public class GoogleMeetBatchDetails {

    @Id
    @SequenceGenerator(name = "google_batch_id_gen", initialValue = 1, allocationSize = 1)
    @GeneratedValue(generator = "google_batch_id_gen")
    @Column(name = "batchId")
    private Long batchId;

    @Column(name = "startDateTime")
    private LocalDateTime startDateTime;

    @Column(name = "endDateTime")
    private LocalDateTime endDateTime;

    @Column(name = "lastSuccessfulExecutionDateTime")
    private LocalDateTime lastSuccessfulExecutionDateTime;

    @Column(name = "status")
    private String status; 

    @Column(name = "batchProcessFailureReason", length = 1000)
    private String batchProcessFailureReason;
}
