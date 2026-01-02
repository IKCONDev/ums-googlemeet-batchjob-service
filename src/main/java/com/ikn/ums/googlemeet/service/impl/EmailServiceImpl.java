package com.ikn.ums.googlemeet.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ikn.ums.googlemeet.enums.ProgressStatus;
import com.ikn.ums.googlemeet.service.EmailService;
import com.ikn.ums.googlemeet.service.EmailTemplateService;
import com.ikn.ums.googlemeet.utils.EmailUtility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private EmailUtility emailUtility;

    @Autowired
    private EmailTemplateService emailTemplateService;
    
    private static final DateTimeFormatter EMAIL_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a");

    @Async("emailTaskExecutor")
    @Override
    public void sendBatchStatusEmailToDEV(
            String[] emails,
            String batchProcessType,
            ProgressStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        sendBatchStatusEmail(emails,batchProcessType, status, startDate, endDate, "DEV");
    }

    @Async("emailTaskExecutor")
    @Override
    public void sendBatchStatusEmailToQA(
            String[] emails,
            String batchProcessType,
            ProgressStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        sendBatchStatusEmail(emails,batchProcessType, status, startDate, endDate, "QA");
    }

    @Async("emailTaskExecutor")
    @Override
    public void sendBatchStatusEmailToPROD(
            String[] emails,
            String batchProcessType,
            ProgressStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        sendBatchStatusEmail(emails,batchProcessType , status, startDate, endDate, "PROD");
    }

    private void sendBatchStatusEmail(
            String[] emails,
            String batchProcessType,
            ProgressStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String appEnvironment
    ) {

        log.info("Sending batch status email asynchronously");
        log.info("Running on thread {}", Thread.currentThread().getName());

        if (emails == null || emails.length == 0) {
            log.warn("No valid email recipients provided, skipping batch email");
            return;
        }

        if (status == ProgressStatus.IN_PROGRESS) {
            log.info("Batch status IN_PROGRESS skipping email");
            return;
        }

        try {

        	String batchSource = "Googlemeet";
            String templateKey = resolveTemplateKey(status);

            String subject = safeSubject(appEnvironment, batchSource ,templateKey, status);
            String body = safeBody(batchSource,templateKey, status, startDate, endDate);

            String[] to;
            String[] cc;
            String[] bcc;

            try {
                to = Optional.ofNullable(
                        emailTemplateService.getToEmails(templateKey)
                ).orElse(new String[0]);

                cc = Optional.ofNullable(
                        emailTemplateService.getCcEmails(templateKey)
                ).orElse(new String[0]);

                bcc = Optional.ofNullable(
                        emailTemplateService.getBccEmails(templateKey)
                ).orElse(new String[0]);

            } catch (Exception ex) {
                log.warn(
                        "Email recipients not found for templateKey {}, using trigger emails only",
                        templateKey
                );
                to = new String[0];
                cc = new String[0];
                bcc = new String[0];
            }

            to = mergePrimaryEmails(to, emails);

            boolean isEmailSent =
                    emailUtility.sendMail(
                            to,
                            subject,
                            body,
                            cc,
                            bcc,
                            null,
                            false
                    );

            if (!isEmailSent) {
                log.warn(
                        "Batch process email not sent status={}, recipients={}",
                        status, Arrays.toString(emails)
                );
            } else {
                log.info(
                        "Batch process email sent successfully status={}",
                        status
                );
            }

        } catch (Exception e) {
            log.error(
                    "Error while sending batch process email status={}, recipients={}",
                    status, Arrays.toString(emails), e
            );
        }
    }

    private String resolveTemplateKey(ProgressStatus status) {
        return switch (status) {
            case SUCCESS -> "batch.process.success";
            case PARTIAL_SUCCESS -> "batch.process.partial";
            default -> "batch.process.failed";
        };
    }

    private String safeSubject(String applicationEnvironment, String batchSource, String templateKey, ProgressStatus status) {
        try {
            String subject = emailTemplateService.getSubject(templateKey);
            if (subject != null && !subject.isBlank()) {
            	return applicationEnvironment+" "+batchSource+" "+subject;
            }
        } catch (Exception ex) {
            log.warn("Subject not found in DB for key {}, using default", templateKey);
        }
        return defaultSubject(applicationEnvironment, batchSource, status);
    }

    private String safeBody(
    		String batchSource,
            String templateKey,
            ProgressStatus status,
            LocalDateTime start,
            LocalDateTime end
    ) {
        try {
            String body = emailTemplateService.getBody(templateKey, start, end);
            if (body != null && !body.isBlank()) {
                return body;
            }
        } catch (Exception ex) {
            log.warn("Body not found in DB for key {}, using default", templateKey);
        }
        return defaultBody(batchSource,status, start, end);
    }

    private String defaultSubject(String applicationEnvironment, String batchSource, ProgressStatus status) {
        return switch (status) {
            case SUCCESS -> applicationEnvironment+" "+batchSource+" Batch Process Completed Successfully";
            case PARTIAL_SUCCESS -> applicationEnvironment+" "+batchSource+" Batch Process Partially Completed";
            default -> applicationEnvironment+" "+batchSource+" Batch Process Failed";
        };
    }

    private String defaultBody(
            String batchSource,
            ProgressStatus status,
            LocalDateTime start,
            LocalDateTime end
    ) {
        long duration =
                (start != null && end != null)
                        ? ChronoUnit.SECONDS.between(start, end)
                        : 0;

        String startTime =
                start != null ? start.format(EMAIL_DATE_FORMAT) : "N/A";

        String endTime =
                end != null ? end.format(EMAIL_DATE_FORMAT) : "N/A";

        return String.format(
                """
                Batch Source : %s
                Batch Status : %s
                Start Time   : %s
                End Time     : %s
                Duration     : %d seconds
                """,
                batchSource,
                status,
                startTime,
                endTime,
                duration
        );
    }



    private String[] mergePrimaryEmails(String[] to, String[] primaryEmails) {

        log.info(
                "Merging email recipients. primaryEmails={}, templateTo={}",
                Arrays.toString(primaryEmails),
                Arrays.toString(to)
        );

        Set<String> merged = new LinkedHashSet<>();

        if (primaryEmails != null && primaryEmails.length > 0) {
            merged.addAll(Arrays.asList(primaryEmails));
        }

        if (to != null && to.length > 0) {
            merged.addAll(Arrays.asList(to));
        }

        String[] result = merged.toArray(new String[0]);

        log.info(
                "Final merged email recipients={}",
                Arrays.toString(result)
        );

        return result;
    }

}
