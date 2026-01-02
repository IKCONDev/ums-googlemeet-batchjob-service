package com.ikn.ums.googlemeet.service;

import java.time.LocalDateTime;

import com.ikn.ums.googlemeet.enums.ProgressStatus;

public interface EmailService {

    void sendBatchStatusEmailToQA(
            String[] emails,
            String batchProcessType,
            ProgressStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    void sendBatchStatusEmailToDEV(
            String[] emails,
            String batchProcessType,
            ProgressStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    void sendBatchStatusEmailToPROD(
            String[] emails,
            String batchProcessType,
            ProgressStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}
