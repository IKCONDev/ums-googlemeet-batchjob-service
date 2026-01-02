package com.ikn.ums.googlemeet.service.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.ikn.ums.googlemeet.client.EmployeeServiceClient;
import com.ikn.ums.googlemeet.dto.BatchExecutionDetailDto;
import com.ikn.ums.googlemeet.dto.BatchExecutionResult;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.entity.GoogleScheduledMeeting;
import com.ikn.ums.googlemeet.entity.GoogleScheduledMeetingAttendee;
import com.ikn.ums.googlemeet.enums.ProgressStatus;
import com.ikn.ums.googlemeet.externaldto.EmployeeDto;
import com.ikn.ums.googlemeet.externaldto.UMSScheduledMeetingDto;
import com.ikn.ums.googlemeet.mapper.GoogleMeetingMapper;
import com.ikn.ums.googlemeet.service.BatchExecutionDetailService;
import com.ikn.ums.googlemeet.service.GoogleAsyncService;
import com.ikn.ums.googlemeet.service.GoogleMeetingPersistenceService;
import com.ikn.ums.googlemeet.service.GoogleMeetingsQueuePublisherService;
import com.ikn.ums.googlemeet.service.GoogleScheduledMeetingService;
import com.ikn.ums.googlemeet.utils.AbstractBatchExecutor;
import com.ikn.ums.googlemeet.utils.BatchEmailNotifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for processing Google Scheduled Meetings.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleScheduledMeetingServiceImpl
        extends AbstractBatchExecutor
        implements GoogleScheduledMeetingService {

    private static final int BATCH_SIZE = 10;

    private final EmployeeServiceClient employeeServiceClient;
    private final GoogleMeetingMapper meetingMapper;
    private final GoogleMeetingPersistenceService persistenceService;
    private final GoogleMeetingsQueuePublisherService queuePublisherService;
    private final GoogleAsyncService googleAsyncService;
    private final Environment environment;
    private final BatchEmailNotifier batchEmailNotifier;
    private final BatchExecutionDetailService batchExecutionDetailService;

    @Override
    public List<GoogleScheduledMeetingDto> performScheduledMeetingsRawDataBatchProcessing() {

        final String methodName = "performScheduledMeetingsRawDataBatchProcessing()";
        log.info("{} - STARTED", methodName);

        LocalDateTime batchStartTime = LocalDateTime.now();

        List<EmployeeDto> employees = fetchEligibleEmployees();
        if (employees.isEmpty()) {
            log.warn("{} - No eligible employees found. Batch aborted", methodName);
            return Collections.emptyList();
        }

        final String batchName = "SCHEDULED_MEETINGS_BATCH";
        BatchExecutionDetailDto batch =
                batchExecutionDetailService.startBatch(batchName);

        log.info("{} - Batch started | batchId={} | batchName={}",
                methodName, batch.getId(), batchName);

        int totalUsers = employees.size();

        BatchExecutionResult<EmployeeDto, GoogleScheduledMeetingDto> result = null;
        List<GoogleScheduledMeetingDto> masterDtoList = Collections.emptyList();

        try {
            log.info("{} - Executing scheduled meetings batch", methodName);

            result = executeScheduledMeetingsBatch(employees, batchName);
            masterDtoList = result.getSuccessResults();

            log.info("{} - Execution completed | successUsers={} | failedUsers={}",
                    methodName,
                    result.getSuccessItems().size(),
                    result.getFailedItems().size());

            attachBatchId(masterDtoList, batch.getId());
            log.info("{} -> Batch Id {} attached to scheduled meetings",
                    methodName, batch.getId());

            persistScheduledMeetings(masterDtoList);
            log.info("{} - Meetings persisted | count={}",
                    methodName, masterDtoList.size());

            publishScheduledMeetings(masterDtoList, batch.getId());
            log.info("{} - Meetings published to queue", methodName);

            return masterDtoList;

        } catch (Exception ex) {
            log.error("{} - Batch execution FAILED", methodName, ex);
            throw ex;

        } finally {
            if (result != null) {
                log.info("{} - Finalizing batch status", methodName);

                completeBatchAndNotify(
                        batch,
                        result,
                        totalUsers,
                        batchName,
                        batchStartTime
                );

                log.info("{} - Batch finalized successfully", methodName);
            }
        }
    }

    private void attachBatchId(List<GoogleScheduledMeetingDto> meetings, Long batchId) {
        if (meetings == null || meetings.isEmpty()) {
            return;
        }

        for (GoogleScheduledMeetingDto dto : meetings) {
            if (dto.getBatchId() == null) {
                dto.setBatchId(batchId);
            }
        }
    }

    private List<EmployeeDto> fetchEligibleEmployees() {

        String methodName = "fetchEligibleEmployees()";

        log.info("{} - Fetching employees from Employee Service", methodName);

        List<EmployeeDto> employeeList =
                employeeServiceClient.getEmployeesListFromEmployeeService();

        if (employeeList == null || employeeList.isEmpty()) {
            log.warn("{} - Employee service returned empty list", methodName);
            return Collections.emptyList();
        }

        List<EmployeeDto> employees =
                employeeList.stream()
                        .filter(e -> e.getEmail() != null)
                        .collect(Collectors.toList());

        log.info("{} - Eligible employees count={}",
                methodName, employees.size());

        return employees;
    }

    private BatchExecutionResult<EmployeeDto, GoogleScheduledMeetingDto>
    executeScheduledMeetingsBatch(List<EmployeeDto> employees, String batchName) {

        log.info("Executing batch {} for {} users", batchName, employees.size());

        List<List<EmployeeDto>> employeeBatches =
                partition(employees, BATCH_SIZE);

        log.info("Partitioned users into {} batches (batchSize={})",
                employeeBatches.size(), BATCH_SIZE);

        return executeInBatches(
                employeeBatches,
                googleAsyncService::getScheduledMeetingsAsync,
                batchName
        );
    }

    private void completeBatchAndNotify(
            BatchExecutionDetailDto batch,
            BatchExecutionResult<EmployeeDto, ?> result,
            int totalUsers,
            String batchName,
            LocalDateTime batchStartTime) {

        int successfulUsers = result.getSuccessItems().size();
        int failedUsers = result.getFailedItems().size();
        int totalMeetingsProcessed = result.getSuccessResults().size();

        ProgressStatus finalStatus =
                calculateBatchStatus(result);

        log.info("Batch {} completed | status={} | success={} | failed={}",
                batchName, finalStatus, successfulUsers, failedUsers);

        List<String> failedEmails =
                extractEmails(result.getFailedItems(), EmployeeDto::getEmail);

        List<String> successEmails =
                extractEmails(result.getSuccessItems(), EmployeeDto::getEmail);

        batch.setStatus(finalStatus.name());
        batch.setTotalUsers(totalUsers);
        batch.setSuccessfulUsers(successfulUsers);
        batch.setFailedUsers(failedUsers);
        batch.setFailedUserEmails(failedEmails);
        batch.setSuccessfulUserEmails(successEmails);
        batch.setRecordsProcessed(totalMeetingsProcessed);

        batchExecutionDetailService.completeBatch(batch);

        log.info("Batch {} status persisted in DB", batchName);

        sendBatchEmail(batchName, finalStatus, batchStartTime);
    }

    private void sendBatchEmail(
            String batchName,
            ProgressStatus status,
            LocalDateTime batchStartTime) {

        log.info("Preparing batch email | batchName={} | status={}", batchName, status);

        String envVar = environment.getProperty("ums.environment.variable");
        String env = envVar != null ? System.getenv(envVar) : null;
        String emails = environment.getProperty("batch.process.trigger-email");

        if (emails == null) {
            log.warn("No batch email recipients configured");
            return;
        }

        String[] emailList =
                Arrays.stream(emails.split("\\s*,\\s*"))
                        .filter(e -> !e.isBlank())
                        .toArray(String[]::new);

        if (emailList.length == 0) {
            log.warn("Batch email list empty after filtering");
            return;
        }

        batchEmailNotifier.sendBatchStatusEmailByEnv(
                env,
                batchName,
                emailList,
                status,
                batchStartTime,
                LocalDateTime.now()
        );

        log.info("Batch email sent | batchName={} | status={}", batchName, status);
    }

    private void persistScheduledMeetings(List<GoogleScheduledMeetingDto> dtos) {

        if (dtos == null || dtos.isEmpty()) {
            log.warn("persistScheduledMeetings() - No meetings to persist");
            return;
        }

        log.info("persistScheduledMeetings() - Mapping {} DTOs to entities", dtos.size());

        List<GoogleScheduledMeeting> entities =
                meetingMapper.toGoogleScheduledEntityList(dtos);

        for (GoogleScheduledMeeting meeting : entities) {
            if (meeting.getAttendees() != null && !meeting.getAttendees().isEmpty()) {
                for (GoogleScheduledMeetingAttendee attendee : meeting.getAttendees()) {
                    attendee.setMeeting(meeting);
                }
            }
        }

        log.info("persistScheduledMeetings() - Persisting {} meetings", entities.size());

        persistenceService.deleteResetAndPersist(entities);

        log.info("persistScheduledMeetings() - Persistence completed successfully");
    }

    private void publishScheduledMeetings(List<GoogleScheduledMeetingDto> dtos, Long batchId) {

        if (dtos == null || dtos.isEmpty()) {
            log.warn("publishScheduledMeetings() - No meetings to publish");
            return;
        }

        log.info("publishScheduledMeetings() - Converting {} meetings to UMS DTOs", dtos.size());

        List<UMSScheduledMeetingDto> umsDtos = meetingMapper.toUMSScheduledDtoList(dtos);

        log.info("publishScheduledMeetings() - Publishing {} meetings to queue", umsDtos.size());

        queuePublisherService.publishScheduledMeetingsBatchEventInQueue(umsDtos, batchId);

        log.info("publishScheduledMeetings() - Queue publish successful");
    }
}
