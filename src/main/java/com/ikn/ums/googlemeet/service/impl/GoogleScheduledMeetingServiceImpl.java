package com.ikn.ums.googlemeet.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.ikn.ums.googlemeet.client.EmployeeServiceClient;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.entity.GoogleScheduledMeeting;
import com.ikn.ums.googlemeet.externaldto.EmployeeDto;
import com.ikn.ums.googlemeet.externaldto.UMSScheduledMeetingDto;
import com.ikn.ums.googlemeet.mapper.GoogleMeetingMapper;
import com.ikn.ums.googlemeet.pipeline.MeetingPipeline;
import com.ikn.ums.googlemeet.processor.GoogleScheduledMeetingProcessor;
import com.ikn.ums.googlemeet.service.GoogleCalendarService;
import com.ikn.ums.googlemeet.service.GoogleMeetingPersistenceService;
import com.ikn.ums.googlemeet.service.GoogleScheduledMeetingService;

import lombok.extern.slf4j.Slf4j;

/**
 * Google Meet scheduled meetings batch-processing implementation.
 */
@Slf4j
@Service
public class GoogleScheduledMeetingServiceImpl implements GoogleScheduledMeetingService {

    @Autowired
    private EmployeeServiceClient employeeServiceClient;

    @Autowired
    private GoogleMeetingMapper googleMeetingMapper;

    @Value("${google.meetings.meeting-details.url}")
    private String meetingDetailsUrl;

    @Autowired
    private GoogleScheduledMeetingProcessor scheduledMeetingProcessor;

    @Autowired
    private GoogleCalendarService googlecalendarService;

    @Autowired
    private GoogleMeetingPersistenceService googleMeetingsPersistenceService;

    private static final ZoneId ZONE = ZoneId.of("Asia/Calcutta");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public List<GoogleScheduledMeetingDto> performScheduledMeetingsRawDataBatchProcessing() {

        String methodName = "performScheduledMeetingsRawDataBatchProcessing()";
        log.info("{} - STARTED", methodName);

        // Fetch employees (or hardcoded emails)
        List<EmployeeDto> employeeList = employeeServiceClient.getEmployeesListFromEmployeeService();
        log.info("{} - Employee list fetched: {}", methodName, employeeList);

        // For testing, fallback to a hardcoded email list
        List<String> emailIds = employeeList.isEmpty()
                ? List.of("siri.chenimini1@gmail.com")
                : employeeList.stream().map(EmployeeDto::getEmail).collect(Collectors.toList());

        List<CompletableFuture<List<GoogleScheduledMeetingDto>>> futuresList = new ArrayList<>();

        for (String emailId : emailIds) {
            futuresList.add(getScheduledMeetingsAsync(emailId));
        }

        // Wait for all async calls to complete
        CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[0])).join();

        // Merge results
        List<GoogleScheduledMeetingDto> masterDtoList =
                futuresList.stream()
                        .map(CompletableFuture::join)
                        .flatMap(List::stream)
                        .collect(Collectors.toList());

        // Filter only upcoming events within next 1 month
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDateTime plusOneMonth = now.plusMonths(1);

        masterDtoList = masterDtoList.stream()
                .filter(m -> {
                    if (m.getStartTime() == null) return false;
                    try {
                        LocalDateTime startTime = LocalDateTime.parse(m.getStartTime(), FORMATTER);
                        return startTime.isAfter(now) && startTime.isBefore(plusOneMonth);
                    } catch (Exception e) {
                        log.warn("Failed to parse startTime: {}", m.getStartTime());
                        return false;
                    }
                })
                .collect(Collectors.toList());

        // Convert DTO → Entity
        List<GoogleScheduledMeeting> masterEntityList =
                googleMeetingMapper.toGoogleScheduledEntityList(masterDtoList);

        // Reset DB entries then persist fresh records
        List<GoogleScheduledMeeting> persistedList =
                googleMeetingsPersistenceService.deleteResetAndPersist(masterEntityList);

        // Convert back to DTO
        masterDtoList = googleMeetingMapper.toGoogleScheduledDtoList(persistedList);

        // Convert to UMS DTO format
        List<UMSScheduledMeetingDto> umsDtoList =
                googleMeetingMapper.toUMSDtoScheduledList(masterDtoList);

        log.info("{} - COMPLETED. Total scheduled meetings saved: {}",
                methodName, masterEntityList.size());

        return masterDtoList;
    }

    /**
     * Async wrapper to fetch per-user meetings concurrently.
     */
    @Async("googleScheduledMeetingApiExecutor")
    public CompletableFuture<List<GoogleScheduledMeetingDto>> getScheduledMeetingsAsync(String userEmail) {

        String methodName = "getScheduledMeetingsAsync()";
        String thread = Thread.currentThread().getName();
        log.info("{} - STARTED async for user: {} on thread: {}", methodName, userEmail, thread);

        List<GoogleScheduledMeetingDto> result = getScheduledMeetings(userEmail);

        log.info("{} - COMPLETED async for user: {} on thread: {}. Fetched {} meetings.",
                methodName, userEmail, thread, result.size());

        return CompletableFuture.completedFuture(result);
    }

    /**
     * Core Google API fetch + processing pipeline.
     */
    private List<GoogleScheduledMeetingDto> getScheduledMeetings(String userEmail) {

        String methodName = "getScheduledMeetings()";

        try {
            // Fetch meetings from Google API
            List<GoogleScheduledMeetingDto> meetings = googlecalendarService.fetchScheduledMeetings(userEmail);

            // Process meetings through pipeline
            return MeetingPipeline
                    .start(meetings, scheduledMeetingProcessor)
                    .preProcess()
                    .classifyType()
                    .attachInvitees()
                    .done();

        } catch (HttpClientErrorException ex) {
            log.error("{} - Client error {} for {}: {}",
                    methodName, ex.getStatusCode(), userEmail, ex.getResponseBodyAsString());
            return Collections.emptyList();

        } catch (Exception ex) {
            log.error("{} - Unrecoverable error for {}: {}",
                    methodName, userEmail, ex.getMessage());
            return Collections.emptyList();
        }
    }
}
