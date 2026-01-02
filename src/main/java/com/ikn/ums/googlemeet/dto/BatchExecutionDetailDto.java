package com.ikn.ums.googlemeet.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchExecutionDetailDto {

    private Long id;
    private String batchName;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer recordsProcessed;
    private Integer totalUsers;
    private Integer successfulUsers;
    private Integer failedUsers;
    private List<String> failedUserEmails;
    private List<String> successfulUserEmails;
    private String errorMessage;
    private LocalDateTime lastSuccessfulAt;
    private LocalDateTime createdAt;
}
