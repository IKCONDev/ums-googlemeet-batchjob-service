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
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.entity.GoogleCompletedMeeting;
import com.ikn.ums.googlemeet.enums.BatchProcessStatus;
import com.ikn.ums.googlemeet.enums.EmployeeStatus;
import com.ikn.ums.googlemeet.enums.ProgressStatus;
import com.ikn.ums.googlemeet.externaldto.EmployeeDto;
import com.ikn.ums.googlemeet.externaldto.UMSCompletedMeetingDto;
import com.ikn.ums.googlemeet.mapper.GoogleMeetingMapper;
import com.ikn.ums.googlemeet.service.BatchExecutionDetailService;
import com.ikn.ums.googlemeet.service.GoogleAsyncService;
import com.ikn.ums.googlemeet.service.GoogleCompletedMeetingService;
import com.ikn.ums.googlemeet.service.GoogleMeetingPersistenceService;
import com.ikn.ums.googlemeet.service.GoogleMeetingsQueuePublisherService;
import com.ikn.ums.googlemeet.utils.AbstractBatchExecutor;
import com.ikn.ums.googlemeet.utils.BatchEmailNotifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for processing completed Google Meet meetings.
 * Mirrors Zoom CompletedMeetingServiceImpl functionality.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCompletedMeetingServiceImpl
        extends AbstractBatchExecutor
        implements GoogleCompletedMeetingService {

    private static final int BATCH_SIZE = 10;

    private final EmployeeServiceClient employeeServiceClient;
    private final GoogleMeetingMapper googleMeetingMapper;
    private final GoogleMeetingPersistenceService googleMeetingPersistenceService;
    private final GoogleMeetingsQueuePublisherService meetingsQueuePublisherService;
    private final GoogleAsyncService googleAsyncService;
    private final Environment environment;
    private final BatchEmailNotifier batchEmailNotifier;
    private final BatchExecutionDetailService batchExecutionDetailService;

    @Override
    public List<GoogleCompletedMeetingDto> performMeetingsRawDataBatchProcessing() {

        final String methodName = "performMeetingsRawDataBatchProcessing()";
        log.info("{} -> STARTED", methodName);

        LocalDateTime batchStartTime = LocalDateTime.now();

        List<EmployeeDto> employees = fetchEligibleEmployees();
        if (employees.isEmpty()) {
            log.warn("{} -> No eligible employees found. Batch aborted", methodName);
            sendEmail(ProgressStatus.PARTIAL_SUCCESS, batchStartTime, methodName);
            return Collections.emptyList();
        }

        final String batchName = "COMPLETED_MEETINGS_BATCH";
        BatchExecutionDetailDto batch = batchExecutionDetailService.startBatch(batchName);

        log.info("{} -> Batch started | batchId={} | batchName={} | totalUsers={}",
                methodName, batch.getId(), batchName, employees.size());

        int totalUsers = employees.size();
        BatchExecutionResult<EmployeeDto, GoogleCompletedMeetingDto> result = null;
        List<GoogleCompletedMeetingDto> masterList = Collections.emptyList();

        try {
            result = executeCompletedMeetingsBatch(employees, batchName);
            masterList = result.getSuccessResults();

            log.info("{} -> Execution completed | successUsers={} | failedUsers={}",
                    methodName,
                    result.getSuccessItems().size(),
                    result.getFailedItems().size());

            attachBatchId(masterList, batch.getId());
            log.info("{} -> Batch Id {} attached to completed meetings",
                    methodName, batch.getId());

            persistCompletedMeetings(masterList);
            log.info("{} -> Completed meetings persisted | count={}", methodName, masterList.size());

            publishCompletedMeetings(masterList, batch.getId());
            log.info("{} -> Completed meetings published to queue", methodName);

            return masterList;

        } catch (Exception ex) {
            log.error("{} -> Completed meetings batch FAILED", methodName, ex);
            throw ex;
        } finally {
            if (result != null) {
                completeBatchAndNotify(batch, result, totalUsers, batchName, batchStartTime);
                log.info("{} -> Completed meetings batch finalized", methodName);
            }
        }
    }

    private void attachBatchId(List<GoogleCompletedMeetingDto> meetings, Long batchId) {
        if (meetings == null || meetings.isEmpty()) return;
        meetings.forEach(dto -> { if (dto.getBatchId() == null) dto.setBatchId(batchId); });
    }

    private BatchExecutionResult<EmployeeDto, GoogleCompletedMeetingDto> executeCompletedMeetingsBatch(
            List<EmployeeDto> employees, String batchName) {

        List<List<EmployeeDto>> employeeBatches = partition(employees, BATCH_SIZE);
        log.info("Partitioned users into {} batches (batchSize={})", employeeBatches.size(), BATCH_SIZE);

        return executeInBatches(employeeBatches, googleAsyncService::getCompletedMeetingsAsync, batchName);
    }

    private void persistCompletedMeetings(List<GoogleCompletedMeetingDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            log.warn("persistCompletedMeetings() - No meetings to persist");
            return;
        }

        List<GoogleCompletedMeeting> entities = googleMeetingMapper.toGoogleCompletedEntityList(dtos);
        linkMeetingToChildEntities(entities);
        googleMeetingPersistenceService.persistCompletedMeetings(entities);
    }

    private void publishCompletedMeetings(List<GoogleCompletedMeetingDto> dtos, Long batchId) {
        if (dtos == null || dtos.isEmpty()) return;
        List<UMSCompletedMeetingDto> umsDtos = googleMeetingMapper.toUMSCompletedDtoList(dtos);
        meetingsQueuePublisherService.publishCompletedMeetingsBatchEventInQueue(umsDtos, batchId);
    }

    private void linkMeetingToChildEntities(List<GoogleCompletedMeeting> meetings) {
        if (meetings == null || meetings.isEmpty()) return;
        meetings.forEach(meeting -> {
            if (meeting.getAttendees() != null) meeting.getAttendees().forEach(a -> a.setMeeting(meeting));
            if (meeting.getParticipants() != null) meeting.getParticipants().forEach(p -> p.setMeeting(meeting));
            if (meeting.getTranscripts() != null) meeting.getTranscripts().forEach(t -> t.setMeeting(meeting));
        });
    }

    private void completeBatchAndNotify(BatchExecutionDetailDto batch,
                                        BatchExecutionResult<EmployeeDto, ?> result,
                                        int totalUsers,
                                        String batchName,
                                        LocalDateTime batchStartTime) {

        int successfulUsers = result.getSuccessItems().size();
        int failedUsers = result.getFailedItems().size();
        int totalMeetingsProcessed = result.getSuccessResults().size();
        ProgressStatus finalStatus = calculateBatchStatus(result);

        batch.setStatus(finalStatus.name());
        batch.setTotalUsers(totalUsers);
        batch.setSuccessfulUsers(successfulUsers);
        batch.setFailedUsers(failedUsers);
        batch.setFailedUserEmails(extractEmails(result.getFailedItems(), EmployeeDto::getEmail));
        batch.setSuccessfulUserEmails(extractEmails(result.getSuccessItems(), EmployeeDto::getEmail));
        batch.setRecordsProcessed(totalMeetingsProcessed);

        batchExecutionDetailService.completeBatch(batch);

        sendEmail(finalStatus, batchStartTime, "completeBatchAndNotify()");
    }

    private void sendEmail(ProgressStatus emailStatus, LocalDateTime batchStartTime, String methodName) {
        String umsAppEnv = environment.getProperty("ums.environment.variable") != null
                ? System.getenv(environment.getProperty("ums.environment.variable"))
                : null;

        String emails = environment.getProperty("batch.process.trigger-email");
        String[] emailList = emails != null
                ? Arrays.stream(emails.split("\\s*,\\s*")).filter(e -> !e.isBlank()).toArray(String[]::new)
                : new String[0];

        if (emailList.length > 0) {
            batchEmailNotifier.sendBatchStatusEmailByEnv(
                    umsAppEnv,
                    "COMPLETED MEETINGS BATCH",
                    emailList,
                    emailStatus,
                    batchStartTime,
                    LocalDateTime.now()
                   // methodName
            );
        }
    }

    private List<EmployeeDto> fetchEligibleEmployees() {
        List<EmployeeDto> employeeList = employeeServiceClient.getEmployeesListFromEmployeeService();
        if (employeeList == null || employeeList.isEmpty()) return Collections.emptyList();

        return employeeList.stream()
                .filter(e -> e.getEmail() != null)
                .filter(e -> BatchProcessStatus.ENABLED == BatchProcessStatus.from(e.getBatchProcessStatus())
                        && EmployeeStatus.ACTIVE == EmployeeStatus.from(e.getEmployeeStatus()))
                .collect(Collectors.toList());
    }
}
