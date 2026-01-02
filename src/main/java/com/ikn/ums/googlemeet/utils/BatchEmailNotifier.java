package com.ikn.ums.googlemeet.utils;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.ikn.ums.googlemeet.enums.ProgressStatus;
import com.ikn.ums.googlemeet.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchEmailNotifier {

    private final EmailService emailService;

    public void sendBatchStatusEmailByEnv(
            String umsAppEnv,
            String batchProcessType,
            String[] emails,
            ProgressStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime
//            String methodName
    ) {
    	String methodName = "sendBatchStatusEmailByEnv";

        try {
            if (umsAppEnv == null || umsAppEnv.length() == 0 ) {
                log.warn("{} - umsAppEnv is null, skipping batch email", methodName);
                return;
            }

            if (emails == null || emails.length == 0) {
                log.warn("{} - No email recipients provided, skipping batch email", methodName);
                return;
            }

            switch (umsAppEnv.toUpperCase()) {

                case "DEV" -> emailService.sendBatchStatusEmailToDEV(
                        emails,batchProcessType, status, startTime, endTime
                );

                case "QA" -> emailService.sendBatchStatusEmailToQA(
                        emails, batchProcessType,status, startTime, endTime
                );

                case "PROD" -> emailService.sendBatchStatusEmailToPROD(
                        emails,batchProcessType, status, startTime, endTime
                );

                default -> log.warn(
                        "{} - Unknown umsAppEnv={}, skipping batch email",
                        methodName, umsAppEnv
                );
            }

        } catch (Exception ex) {
            log.warn(
                    "{} - Batch status email failed for env={}, reason={}",
                    methodName, umsAppEnv, ex.getMessage()
            );
        }
    }
}

