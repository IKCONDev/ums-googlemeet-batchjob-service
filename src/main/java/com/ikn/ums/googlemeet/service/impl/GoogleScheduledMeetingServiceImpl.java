package com.ikn.ums.googlemeet.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ikn.ums.googlemeet.client.EmployeeServiceClient;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.entity.GoogleScheduledMeeting;
import com.ikn.ums.googlemeet.entity.GoogleScheduledMeetingAttendee;
import com.ikn.ums.googlemeet.externaldto.EmployeeDto;
import com.ikn.ums.googlemeet.externaldto.UMSScheduledMeetingDto;
import com.ikn.ums.googlemeet.mapper.GoogleMeetingMapper;
import com.ikn.ums.googlemeet.processor.GoogleScheduledMeetingProcessor;
import com.ikn.ums.googlemeet.service.GoogleAsyncService;
import com.ikn.ums.googlemeet.service.GoogleMeetingPersistenceService;
import com.ikn.ums.googlemeet.service.GoogleMeetingsQueuePublisherService;
import com.ikn.ums.googlemeet.service.GoogleScheduledMeetingService;
import com.ikn.ums.googlemeet.utils.AbstractBatchExecutor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GoogleScheduledMeetingServiceImpl
        extends AbstractBatchExecutor
        implements GoogleScheduledMeetingService {

    private static final int BATCH_SIZE = 20;

    @Autowired
    private EmployeeServiceClient employeeServiceClient;

    @Autowired
    private GoogleAsyncService googleAsyncService;

    @Autowired
    private GoogleScheduledMeetingProcessor meetingProcessor;

    @Autowired
    private GoogleMeetingMapper meetingMapper;

    @Autowired
    private GoogleMeetingPersistenceService persistenceService;

    @Autowired
    private GoogleMeetingsQueuePublisherService queuePublisherService;

    @Override
    public List<GoogleScheduledMeetingDto> performScheduledMeetingsRawDataBatchProcessing() {

        String method = "performScheduledMeetingsRawDataBatchProcessing()";
        log.info("{} - STARTED", method);

        // 1. Fetch employees
        List<EmployeeDto> employees = employeeServiceClient.getEmployeesListFromEmployeeService();
        log.info("{} - Fetched employees - count={}", method, employees != null ? employees.size() : 0);

        if (employees == null || employees.isEmpty()) {
            log.warn("{} - No employees found. Skipping batch.", method);
            return Collections.emptyList();
        }

        // 2. Filter active users with email
        List<EmployeeDto> eligibleEmployees = employees.stream()
                .filter(e -> e.getEmail() != null)
                .collect(Collectors.toList());

        if (eligibleEmployees.isEmpty()) {
            log.warn("{} - No eligible employees found.", method);
            return Collections.emptyList();
        }

        log.info("{} - Eligible employees - count={}", method, eligibleEmployees.size());

        // 3. Partition into batches
        List<List<EmployeeDto>> employeeBatches = partition(eligibleEmployees, BATCH_SIZE);
        log.info("{} - Partitioned {} users into {} batch(es) with batchSize={}",
                method, eligibleEmployees.size(), employeeBatches.size(), BATCH_SIZE);

        // 4. Execute async Google API calls
        List<GoogleScheduledMeetingDto> masterDtoList =
                executeInBatches(
                        employeeBatches,
                        googleAsyncService::getScheduledMeetingsAsync,
                        "GoogleScheduledMeetingsBatch"
                );

        log.info("{} - Async fetch completed. Total meetings fetched={}", method, masterDtoList.size());

        if (masterDtoList.isEmpty()) {
            log.warn("{} - No scheduled meetings fetched from Google.", method);
            return Collections.emptyList();
        }

        List<GoogleScheduledMeeting> entities = meetingMapper.toGoogleScheduledEntityList(masterDtoList);

        // Fix bidirectional attendee relationship
        for (GoogleScheduledMeeting meeting : entities) {
            if (meeting.getAttendees() != null) {
                for (GoogleScheduledMeetingAttendee attendee : meeting.getAttendees()) {
                    attendee.setMeeting(meeting);
                }
            }
        }
        log.info("{} - Linked attendees to meeting entities", method);

        // 8. Persist (delete + reset + insert)
        List<GoogleScheduledMeeting> persisted = persistenceService.deleteResetAndPersist(entities);
        log.info("{} - Persisted scheduled meetings - savedCount={}", method,
                persisted != null ? persisted.size() : 0);

        // 9. Map back to DTO
        List<GoogleScheduledMeetingDto> persistedDtos = meetingMapper.toGoogleScheduledDtoList(persisted);

        // 10. Convert to UMS DTO
        List<UMSScheduledMeetingDto> umsDtos = meetingMapper.toUMSScheduledDtoList(persistedDtos);
        log.info("{} - Converted to UMS DTOs - count={}", method, umsDtos.size());

        // 11. Publish to queue
        queuePublisherService.publishScheduledMeetingsBatchEventInQueue(umsDtos);
        log.info("{} - Published scheduled meetings batch event - count={}", method, umsDtos.size());

        log.info("{} - COMPLETED successfully", method);
        return persistedDtos;
    }
}
