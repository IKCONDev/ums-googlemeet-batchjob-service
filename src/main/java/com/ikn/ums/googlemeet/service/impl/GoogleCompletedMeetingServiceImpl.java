package com.ikn.ums.googlemeet.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ikn.ums.googlemeet.client.EmployeeServiceClient;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.entity.GoogleCompletedMeeting;
import com.ikn.ums.googlemeet.enums.BatchProcessStatus;
import com.ikn.ums.googlemeet.enums.EmployeeStatus;
import com.ikn.ums.googlemeet.externaldto.EmployeeDto;
import com.ikn.ums.googlemeet.externaldto.UMSCompletedMeetingDto;
import com.ikn.ums.googlemeet.mapper.GoogleMeetingMapper;
import com.ikn.ums.googlemeet.service.GoogleAsyncService;
import com.ikn.ums.googlemeet.service.GoogleCompletedMeetingService;
import com.ikn.ums.googlemeet.service.GoogleMeetingPersistenceService;
import com.ikn.ums.googlemeet.service.GoogleMeetingsQueuePublisherService;
import com.ikn.ums.googlemeet.utils.AbstractBatchExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * Google Meet completed meetings batch processor
 *
 * Flow :
 * 1. Fetch employees
 * 2. Filter eligible employees
 * 3. Fetch completed meetings async per employee
 * 4. Persist RAW meetings
 * 5. Publish ENRICHED in-memory DTOs to UMS
 */
@Slf4j
@Service
public class GoogleCompletedMeetingServiceImpl
        extends AbstractBatchExecutor
        implements GoogleCompletedMeetingService {

    @Autowired
    private EmployeeServiceClient employeeServiceClient;

    @Autowired
    private GoogleMeetingMapper googleMeetingMapper;

    @Autowired
    private GoogleMeetingPersistenceService googleMeetingPersistenceService;

    @Autowired
    private GoogleMeetingsQueuePublisherService meetingsQueuePublisherService;

    @Autowired
    private GoogleAsyncService googleAsyncService;

    private static final int BATCH_SIZE = 10; 

    @Override
    public List<GoogleCompletedMeetingDto> performMeetingsRawDataBatchProcessing() {

        final String methodName = "performMeetingsRawDataBatchProcessing()";
        log.info("{} -> STARTED", methodName);

        List<EmployeeDto> employeeList =
                employeeServiceClient.getEmployeesListFromEmployeeService();

        log.info("{} -> Fetched employee list, count={}",
                methodName, employeeList != null ? employeeList.size() : 0);

        if (employeeList == null || employeeList.isEmpty()) {
            log.warn("{} -> No employees found. Skipping processing.", methodName);
            return Collections.emptyList();
        }

        // ✅ SAME FILTERING AS ZOOM
        List<EmployeeDto> eligibleEmployees =
                employeeList.stream()
                        .filter(e -> e.getEmail() != null)
                        .filter(e ->
                                BatchProcessStatus.ENABLED ==
                                        BatchProcessStatus.from(e.getBatchProcessStatus()) &&
                                EmployeeStatus.ACTIVE ==
                                        EmployeeStatus.from(e.getEmployeeStatus()))
                        .collect(Collectors.toList());

        log.info("{} -> Eligible employees count={}",
                methodName, eligibleEmployees.size());

        if (eligibleEmployees.isEmpty()) {
            log.warn("{} -> No eligible employees after filtering.", methodName);
            return Collections.emptyList();
        }

        List<List<EmployeeDto>> employeeBatches =
                partition(eligibleEmployees, BATCH_SIZE);

        log.info("{} -> Partitioned {} employees into {} batch(es), batchSize={}",
                methodName,
                eligibleEmployees.size(),
                employeeBatches.size(),
                BATCH_SIZE);

        // ✅ PASS EmployeeDto (NOT email string)
        List<GoogleCompletedMeetingDto> masterList =
                executeInBatches(
                        employeeBatches,
                        googleAsyncService::getCompletedMeetingsAsync,
                        "CompletedMeetingsBatch");

        log.info("{} -> All batches completed. Total meetings fetched={}",
                methodName, masterList.size());

        if (masterList.isEmpty()) {
            log.warn("{} -> No completed meetings fetched. Exiting.", methodName);
            return Collections.emptyList();
        }

        // Persist RAW meetings 
        List<GoogleCompletedMeeting> meetingEntities =
                googleMeetingMapper.toGoogleCompletedEntityList(masterList);

        log.info("{} -> Mapped DTOs to entities, count={}",
                methodName, meetingEntities.size());

        linkMeetingToChildEntities(meetingEntities);

        List<GoogleCompletedMeeting> savedMeetings =
                googleMeetingPersistenceService.persistCompletedMeetings(meetingEntities);

        log.info("{} -> Persisted completed meetings, savedCount={}",
                methodName, savedMeetings.size());

        // ✅ PUBLISH ENRICHED MASTER LIST 
        List<UMSCompletedMeetingDto> umsDtoList =
                googleMeetingMapper.toUMSCompletedDtoList(masterList);

        meetingsQueuePublisherService
                .publishCompletedMeetingsBatchEventInQueue(umsDtoList);

        log.info("{} -> CompletedMeetingsBatchEvent published, recordCount={}",
                methodName, umsDtoList.size());

        log.info("{} -> COMPLETED successfully", methodName);

        return masterList;
    }

    /**
     * Link meeting to child entities for JPA
     */
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
