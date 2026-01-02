package com.ikn.ums.googlemeet.service.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.ikn.ums.googlemeet.client.EmployeeServiceClient;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.entity.GoogleScheduledMeeting;
import com.ikn.ums.googlemeet.entity.GoogleScheduledMeetingAttendee;
import com.ikn.ums.googlemeet.enums.ProgressStatus;
import com.ikn.ums.googlemeet.externaldto.EmployeeDto;
import com.ikn.ums.googlemeet.externaldto.UMSScheduledMeetingDto;
import com.ikn.ums.googlemeet.mapper.GoogleMeetingMapper;
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
 * Handles async Google API calls, batch execution, persistence,
 * queue publishing, and batch status notification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleScheduledMeetingServiceImpl
        extends AbstractBatchExecutor
        implements GoogleScheduledMeetingService {

    private static final int BATCH_SIZE = 10;

    private final EmployeeServiceClient employeeServiceClient;
    private final GoogleAsyncService googleAsyncService;
    private final GoogleMeetingMapper meetingMapper;
    private final GoogleMeetingPersistenceService persistenceService;
    private final GoogleMeetingsQueuePublisherService queuePublisherService;
    private final Environment environment;
    private final BatchEmailNotifier batchEmailNotifier;

    @Override
    public List<GoogleScheduledMeetingDto> performScheduledMeetingsRawDataBatchProcessing() {

        final String methodName = "performScheduledMeetingsRawDataBatchProcessing()";
        log.info("{} - STARTED", methodName);

        LocalDateTime batchStartTime = LocalDateTime.now();

        List<EmployeeDto> employeeList =
                employeeServiceClient.getEmployeesListFromEmployeeService();

        log.info("{} - Fetched employee list - count={}",
                methodName, employeeList != null ? employeeList.size() : 0);

        if (employeeList == null || employeeList.isEmpty()) {
            log.warn("{} - No employees found. Skipping batch.", methodName);
            return Collections.emptyList();
        }

        List<EmployeeDto> eligibleEmployees = employeeList.stream()
                .filter(e -> e.getEmail() != null)
                .collect(Collectors.toList());

        log.info("{} - Eligible employees - count={}",
                methodName, eligibleEmployees.size());

        if (eligibleEmployees.isEmpty()) {
            log.warn("{} - No eligible employees after filtering.", methodName);
            return Collections.emptyList();
        }


        List<List<EmployeeDto>> employeeBatches =
                partition(eligibleEmployees, BATCH_SIZE);

        log.info("{} - Partitioned {} users into {} batch(es) with batchSize={}",
                methodName,
                eligibleEmployees.size(),
                employeeBatches.size(),
                BATCH_SIZE);

        List<GoogleScheduledMeetingDto> masterDtoList =
                executeInBatches(
                        employeeBatches,
                        googleAsyncService::getScheduledMeetingsAsync,
                        "ScheduledMeetingsBatch"
                );

        log.info("{} - All batches completed. Total scheduled meetings fetched={}",
                methodName, masterDtoList.size());

        boolean noMeetingsFetched = masterDtoList.isEmpty();

        if (noMeetingsFetched) {
            log.warn("{} - No scheduled meetings fetched from Google.", methodName);
        }


        List<GoogleScheduledMeeting> entityList =
                meetingMapper.toGoogleScheduledEntityList(masterDtoList);

        log.info("{} - Mapped DTOs to entity list - count={}",
                methodName, entityList.size());

        for (GoogleScheduledMeeting meeting : entityList) {
            if (meeting.getAttendees() != null && !meeting.getAttendees().isEmpty()) {
                for (GoogleScheduledMeetingAttendee attendee : meeting.getAttendees()) {
                    attendee.setMeeting(meeting);
                }
            }
        }

        List<GoogleScheduledMeeting> persistedMeetings =
                persistenceService.deleteResetAndPersist(entityList);

        log.info("{} - Persisted scheduled meetings - savedCount={}",
                methodName,
                persistedMeetings != null ? persistedMeetings.size() : 0);

        List<GoogleScheduledMeetingDto> savedDtos =
                meetingMapper.toGoogleScheduledDtoList(persistedMeetings);

        List<UMSScheduledMeetingDto> umsDtos =
                meetingMapper.toUMSScheduledDtoList(savedDtos);

        log.info("{} - Converted to UMSScheduledMeetingDto list - count={}",
                methodName,
                umsDtos != null ? umsDtos.size() : 0);


        queuePublisherService
                .publishScheduledMeetingsBatchEventInQueue(umsDtos);

        log.info("{} - ScheduledMeetingsBatchEvent published successfully - recordCount={}",
                methodName,
                umsDtos != null ? umsDtos.size() : 0);

        ProgressStatus emailStatus =
                noMeetingsFetched ? ProgressStatus.PARTIAL_SUCCESS : ProgressStatus.SUCCESS;

        String umsEnvVariableName =
                environment.getProperty("ums.environment.variable");

        String umsAppEnv =
                umsEnvVariableName != null ? System.getenv(umsEnvVariableName) : null;

        String emails =
                environment.getProperty("batch.process.trigger-email");

        String[] emailList =
                emails != null ? emails.split("\\s*,\\s*") : new String[0];

        emailList = Arrays.stream(emailList)
                .filter(e -> e != null && !e.isBlank())
                .toArray(String[]::new);

        log.info("{} -> Email check | env={} | recipients={}",
                methodName,
                umsAppEnv,
                Arrays.toString(emailList));

        if (emailList.length > 0) {
            batchEmailNotifier.sendBatchStatusEmailByEnv(
                    umsAppEnv,
                    "SCHEDULED MEETINGS BATCH",
                    emailList,
                    emailStatus,
                    batchStartTime,
                    LocalDateTime.now(),
                    methodName
            );

            log.info("{} -> Batch status email triggered | status={}",
                    methodName, emailStatus);
        } else {
            log.warn("{} -> No valid email recipients configured, skipping email",
                    methodName);
        }

        log.info("{} - COMPLETED successfully", methodName);

        return savedDtos;
    }
}
