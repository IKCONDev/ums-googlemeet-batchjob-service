package com.ikn.ums.googlemeet.service.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.ikn.ums.googlemeet.client.EmployeeServiceClient;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.entity.GoogleCompletedMeeting;
import com.ikn.ums.googlemeet.enums.BatchProcessStatus;
import com.ikn.ums.googlemeet.enums.EmployeeStatus;
import com.ikn.ums.googlemeet.enums.ProgressStatus;
import com.ikn.ums.googlemeet.externaldto.EmployeeDto;
import com.ikn.ums.googlemeet.externaldto.UMSCompletedMeetingDto;
import com.ikn.ums.googlemeet.mapper.GoogleMeetingMapper;
import com.ikn.ums.googlemeet.service.GoogleAsyncService;
import com.ikn.ums.googlemeet.service.GoogleCompletedMeetingService;
import com.ikn.ums.googlemeet.service.GoogleMeetingPersistenceService;
import com.ikn.ums.googlemeet.service.GoogleMeetingsQueuePublisherService;
import com.ikn.ums.googlemeet.utils.AbstractBatchExecutor;
import com.ikn.ums.googlemeet.utils.BatchEmailNotifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCompletedMeetingServiceImpl
        extends AbstractBatchExecutor
        implements GoogleCompletedMeetingService {

    private final EmployeeServiceClient employeeServiceClient;
    private final GoogleMeetingMapper googleMeetingMapper;
    private final GoogleMeetingPersistenceService googleMeetingPersistenceService;
    private final GoogleMeetingsQueuePublisherService meetingsQueuePublisherService;
    private final GoogleAsyncService googleAsyncService;
    private final Environment environment;
    private final BatchEmailNotifier batchEmailNotifier;

    private static final int BATCH_SIZE = 10;

    @Override
    public List<GoogleCompletedMeetingDto> performMeetingsRawDataBatchProcessing() {

        final String methodName = "performMeetingsRawDataBatchProcessing()";
        log.info("{} -> STARTED", methodName);

        LocalDateTime batchStartTime = LocalDateTime.now();

        List<EmployeeDto> employeeList =
                employeeServiceClient.getEmployeesListFromEmployeeService();

        log.info("{} -> Fetched employee list, count={}",
                methodName, employeeList != null ? employeeList.size() : 0);

        if (employeeList == null || employeeList.isEmpty()) {
            log.warn("{} -> No employees found. Skipping processing.", methodName);
            sendEmail(ProgressStatus.PARTIAL_SUCCESS, batchStartTime, methodName);
            return Collections.emptyList();
        }

        List<EmployeeDto> eligibleEmployees =
                employeeList.stream()
                        .filter(e -> e.getEmail() != null)
                        .filter(e ->
                                BatchProcessStatus.ENABLED ==
                                        BatchProcessStatus.from(e.getBatchProcessStatus())
                                &&
                                EmployeeStatus.ACTIVE ==
                                        EmployeeStatus.from(e.getEmployeeStatus()))
                        .collect(Collectors.toList());

        log.info("{} -> Eligible employees count={}",
                methodName, eligibleEmployees.size());

        if (eligibleEmployees.isEmpty()) {
            log.warn("{} -> No eligible employees after filtering.", methodName);
            sendEmail(ProgressStatus.PARTIAL_SUCCESS, batchStartTime, methodName);
            return Collections.emptyList();
        }

        List<List<EmployeeDto>> employeeBatches =
                partition(eligibleEmployees, BATCH_SIZE);

        log.info("{} -> Partitioned {} employees into {} batch(es), batchSize={}",
                methodName,
                eligibleEmployees.size(),
                employeeBatches.size(),
                BATCH_SIZE);

        List<GoogleCompletedMeetingDto> masterList =
                executeInBatches(
                        employeeBatches,
                        googleAsyncService::getCompletedMeetingsAsync,
                        "CompletedMeetingsBatch");

        log.info("{} -> All batches completed. Total meetings fetched={}",
                methodName, masterList.size());

        boolean noMeetingsFetched = masterList.isEmpty();

        // ---- PERSIST ONLY IF DATA EXISTS ----
        if (!noMeetingsFetched) {

            List<GoogleCompletedMeeting> meetingEntities =
                    googleMeetingMapper.toGoogleCompletedEntityList(masterList);

            log.info("{} -> Mapped DTOs to entities, count={}",
                    methodName, meetingEntities.size());

            linkMeetingToChildEntities(meetingEntities);

            List<GoogleCompletedMeeting> savedMeetings =
                    googleMeetingPersistenceService.persistCompletedMeetings(meetingEntities);

            log.info("{} -> Persisted completed meetings, savedCount={}",
                    methodName, savedMeetings.size());

            List<UMSCompletedMeetingDto> umsDtoList =
                    googleMeetingMapper.toUMSCompletedDtoList(masterList);

            meetingsQueuePublisherService
                    .publishCompletedMeetingsBatchEventInQueue(umsDtoList);

            log.info("{} -> CompletedMeetingsBatchEvent published, recordCount={}",
                    methodName, umsDtoList.size());
        } else {
            log.warn("{} -> No completed meetings fetched. Skipping persistence & publish.",
                    methodName);
        }

        ProgressStatus emailStatus =
                noMeetingsFetched
                        ? ProgressStatus.PARTIAL_SUCCESS
                        : ProgressStatus.SUCCESS;

        sendEmail(emailStatus, batchStartTime, methodName);

        log.info("{} -> COMPLETED successfully", methodName);

        return masterList;
    }

    // ---------------- EMAIL HELPER ----------------

    private void sendEmail(
            ProgressStatus emailStatus,
            LocalDateTime batchStartTime,
            String methodName) {

        String umsApplicationSystemEnv =
                environment.getProperty("ums.environment.variable");

        String umsAppEnv =
                umsApplicationSystemEnv != null
                        ? System.getenv(umsApplicationSystemEnv)
                        : null;

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
                    "COMPLETED MEETINGS BATCH",
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
    }

    // ---------------- LINK CHILD ENTITIES ----------------

    private void linkMeetingToChildEntities(List<GoogleCompletedMeeting> meetings) {

        final String methodName = "linkMeetingToChildEntities()";

        if (meetings == null || meetings.isEmpty()) {
            log.info("{} -> No meeting entities to link.", methodName);
            return;
        }

        log.info("{} -> Linking child entities for {} meetings",
                methodName, meetings.size());

        for (GoogleCompletedMeeting meeting : meetings) {

            if (meeting.getAttendees() != null) {
                meeting.getAttendees().forEach(a -> a.setMeeting(meeting));
            }

            if (meeting.getParticipants() != null) {
                meeting.getParticipants().forEach(p -> p.setMeeting(meeting));
            }

            if (meeting.getTranscripts() != null) {
                meeting.getTranscripts().forEach(t -> t.setMeeting(meeting));
            }
        }

        log.info("{} -> Linking completed.", methodName);
    }
}
