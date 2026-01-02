package com.ikn.ums.googlemeet.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "batch_execution_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchExecutionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_name", nullable = false)
    private String batchName;

    /**
     * IN_PROGRESS | SUCCESS | PARTIAL_SUCCESS | FAILED
     */
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "records_processed")
    private Integer recordsProcessed;

    @Column(name = "total_users")
    private Integer totalUsers;

    @Column(name = "successful_users")
    private Integer successfulUsers;

    @Column(name = "failed_users")
    private Integer failedUsers;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "failed_user_emails", columnDefinition = "jsonb")
    private List<String> failedUserEmails;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "successful_user_emails", columnDefinition = "jsonb")
    private List<String> successfulUserEmails;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "last_successful_at")
    private LocalDateTime lastSuccessfulAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
