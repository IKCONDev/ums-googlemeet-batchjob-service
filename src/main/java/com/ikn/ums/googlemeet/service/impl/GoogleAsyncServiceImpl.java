package com.ikn.ums.googlemeet.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.externaldto.EmployeeDto;
import com.ikn.ums.googlemeet.pipeline.MeetingPipeline;
import com.ikn.ums.googlemeet.processor.GoogleCompletedMeetingProcessor;
import com.ikn.ums.googlemeet.processor.GoogleScheduledMeetingProcessor;
import com.ikn.ums.googlemeet.service.GoogleCalendarService;
import com.ikn.ums.googlemeet.service.GoogleAsyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAsyncServiceImpl implements GoogleAsyncService {

    private final GoogleCalendarService googlecalendarService;

    private final GoogleScheduledMeetingProcessor googleScheduledMeetingProcessor;

    private final GoogleCompletedMeetingProcessor googleCompletedMeetingProcessor;

    @Async("googleScheduledMeetingApiExecutor")
    @Override
    public CompletableFuture<List<GoogleScheduledMeetingDto>> getScheduledMeetingsAsync(EmployeeDto user) {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        String methodName = "getScheduledMeetingsAsync()";

        log.info("{} - STARTED async execution for user: {} on thread: {}",
                methodName, user.getEmail(), threadName);

        List<GoogleScheduledMeetingDto> result = getScheduledMeetings(user);

        long duration = System.currentTimeMillis() - startTime;
        log.info("{} - COMPLETED async execution for user: {} on thread: {} in {} ms",
                methodName, user.getEmail(), threadName, duration);

        return CompletableFuture.completedFuture(result);
    }

    /**
     * Fetch scheduled Google meetings for the given user.
     * Handles API call, pipeline, filtering, and exception handling.
     */
    public List<GoogleScheduledMeetingDto> getScheduledMeetings(EmployeeDto user) {
        final String methodName = "getScheduledMeetings()";
        log.info("{} - Fetching scheduled meetings for userId={}", methodName, user.getEmail());

        try {
            // 1️⃣ Fetch from Google Calendar API
            List<GoogleScheduledMeetingDto> scheduledMeetings =
                    googlecalendarService.fetchScheduledMeetings(user.getEmail());

            log.info("{} - Retrieved {} scheduled meetings from Google for userId={}",
                    methodName, scheduledMeetings.size(), user.getEmail());

            // 2️⃣ Apply date range filter: now → plus 1 month
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Calcutta"));
            LocalDateTime plusOneMonth = now.plusMonths(1);
            log.info("{} - Applying date range filter from {} to {}", methodName, now, plusOneMonth);

            // 3️⃣ Process pipeline: preprocess, filter, classify type, attach invitees
            List<GoogleScheduledMeetingDto> result =
                    MeetingPipeline.start(scheduledMeetings, googleScheduledMeetingProcessor)
                            //.preProcess()
                            .filterDateRange(now, plusOneMonth)
                            .setEmployeeDetails(user)
                            .classifyType()
                            .attachInvitees()
                            .done();
            

            log.info("{} - Pipeline processing complete. Final meeting count={}", methodName, result.size());
            return result;

        } catch (HttpClientErrorException ex) {
            log.error("{} - Client error {} for user {}: {}",
                    methodName, ex.getStatusCode(), user.getEmail(), ex.getResponseBodyAsString());
            return Collections.emptyList();

        } catch (Exception ex) {
            log.error("{} - Unrecoverable error for user {}: {}", methodName, user.getEmail(), ex.getMessage(), ex);
            return Collections.emptyList();
        }
    
    }

    
    @Async("googleMeetingApiExecutor")
    @Override
    public CompletableFuture<List<GoogleCompletedMeetingDto>> getCompletedMeetingsAsync(EmployeeDto user) {

        long start = System.currentTimeMillis();
        log.info("getCompletedMeetingsAsync() - START user {} on thread {}",
                user.getEmail(), Thread.currentThread().getName());

        List<GoogleCompletedMeetingDto> result = getCompletedMeetings(user);

        long duration = System.currentTimeMillis() - start;
        log.info("getCompletedMeetingsAsync() - END user {} Duration {} ms",
                user.getEmail(), duration);

        return CompletableFuture.completedFuture(result);
    }

    public List<GoogleCompletedMeetingDto> getCompletedMeetings(EmployeeDto user) {

        log.info("getCompletedMeetings() - Started fetching completed meetings for userId={}",
                user.getEmail());

        try {
            List<GoogleCompletedMeetingDto> meetings =
                    googlecalendarService.fetchCompletedMeetings(user.getEmail());

            log.info("getCompletedMeetings() - Fetched {} meetings for userId={}",
                    meetings != null ? meetings.size() : 0, user.getEmail());

            List<GoogleCompletedMeetingDto> result =
                    MeetingPipeline.start(meetings, googleCompletedMeetingProcessor)
                            .preProcess()
                            .filterAlreadyProcessed()
                            .classifyType()
                            .setEmployeeDetails(user)   
                            .enrichData()
                            .attachConferenceData()
                            .attachInvitees()
                            .attachParticipants()
                            .attachTranscripts()
                            .done();

            log.info("getCompletedMeetings() - Pipeline completed successfully for userId={}",
                    user.getEmail());

            return result;

        } catch (HttpClientErrorException ex) {
            log.error("getCompletedMeetings() - Client error {} for userId={} -> {}",
                    ex.getStatusCode(), user.getEmail(), ex.getResponseBodyAsString());
            return Collections.emptyList();

        } catch (Exception ex) {
            log.error("getCompletedMeetings() - Unexpected error for userId={} -> {}",
                    user.getEmail(), ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

}
