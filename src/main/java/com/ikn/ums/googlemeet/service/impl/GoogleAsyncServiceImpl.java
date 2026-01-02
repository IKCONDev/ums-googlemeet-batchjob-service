package com.ikn.ums.googlemeet.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.dto.UserExecutionResult;
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

    private final GoogleCalendarService googleCalendarService;
    private final GoogleScheduledMeetingProcessor googleScheduledMeetingProcessor;
    private final GoogleCompletedMeetingProcessor googleCompletedMeetingProcessor;

    
    @Async("googleScheduledMeetingApiExecutor")
    @Override
    public CompletableFuture<UserExecutionResult<GoogleScheduledMeetingDto>> getScheduledMeetingsAsync(EmployeeDto user) {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        String methodName = "getScheduledMeetingsAsync()";

        log.info("{} - STARTED async execution for user: {} on thread: {}", methodName, user.getEmail(), threadName);

        UserExecutionResult<GoogleScheduledMeetingDto> result = getScheduledMeetings(user);

        long duration = System.currentTimeMillis() - startTime;
        log.info("{} - COMPLETED async execution for user: {} on thread: {} in {} ms",
                methodName, user.getEmail(), threadName, duration);

        return CompletableFuture.completedFuture(result);
    }

    public UserExecutionResult<GoogleScheduledMeetingDto> getScheduledMeetings(EmployeeDto user) {
        final String methodName = "getScheduledMeetings()";
        log.info("{} - Fetching scheduled meetings for userId={}", methodName, user.getEmail());

        try {
            List<GoogleScheduledMeetingDto> meetings = googleCalendarService.fetchScheduledMeetings(user.getEmail());

            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Calcutta"));
            LocalDateTime plusOneMonth = now.plusMonths(1);

            List<GoogleScheduledMeetingDto> processed =
                    MeetingPipeline.start(meetings, googleScheduledMeetingProcessor)
                            //.preProcess()
                            .filterDateRange(now, plusOneMonth)
                            .setEmployeeDetails(user)
                            .classifyType()
                            .attachInvitees()
                            .done();

            return UserExecutionResult.success(processed);

        } catch (HttpClientErrorException ex) {
            log.error("{} - Client error {} for user {}: {}",
                    methodName, ex.getStatusCode(), user.getEmail(), ex.getResponseBodyAsString());
            return UserExecutionResult.failure("CLIENT_ERROR -> " + ex.getMessage());

        } catch (ResourceAccessException ex) {
            return UserExecutionResult.failure("NETWORK_ERROR -> " + ex.getMessage());

        } catch (Exception ex) {
            log.error("{} - SYSTEM ERROR for user {}", methodName, user.getEmail(), ex);
            return UserExecutionResult.failure("SYSTEM_ERROR -> " + ex.getMessage());
        }
    }

   
    @Async("googleMeetingApiExecutor")
    @Override
    public CompletableFuture<UserExecutionResult<GoogleCompletedMeetingDto>> getCompletedMeetingsAsync(EmployeeDto user) {
        long startTime = System.currentTimeMillis();
        log.info("getCompletedMeetingsAsync() - START user {} on thread {}", user.getEmail(), Thread.currentThread().getName());

        UserExecutionResult<GoogleCompletedMeetingDto> result = getCompletedMeetings(user);

        long duration = System.currentTimeMillis() - startTime;
        log.info("getCompletedMeetingsAsync() - END user {} Duration {} ms", user.getEmail(), duration);

        return CompletableFuture.completedFuture(result);
    }

    public UserExecutionResult<GoogleCompletedMeetingDto> getCompletedMeetings(EmployeeDto user) {
        final String methodName = "getCompletedMeetings()";
        log.info("{} - Started fetching completed meetings for userId={}", methodName, user.getEmail());

        try {
            List<GoogleCompletedMeetingDto> meetings = googleCalendarService.fetchCompletedMeetings(user.getEmail());

            List<GoogleCompletedMeetingDto> processed =
                    MeetingPipeline.start(meetings, googleCompletedMeetingProcessor)
                            .preProcess()
                            .filterAlreadyProcessed()
                            .classifyType()
                            .setEmployeeDetails(user)
                            .enrichData()
                            .attachInvitees()
                            .attachConferenceData()
                            .attachParticipants()
                            .attachTranscripts()
                            .done();

            return UserExecutionResult.success(processed);

        } catch (HttpClientErrorException ex) {
            log.error("{} - Client error {} for user {}: {}", methodName, ex.getStatusCode(), user.getEmail(), ex.getResponseBodyAsString());
            return UserExecutionResult.failure("CLIENT_ERROR -> " + ex.getMessage());

        } catch (ResourceAccessException ex) {
            return UserExecutionResult.failure("NETWORK_ERROR -> " + ex.getMessage());

        } catch (Exception ex) {
            log.error("{} - SYSTEM ERROR for user {}", methodName, user.getEmail(), ex);
            return UserExecutionResult.failure("SYSTEM_ERROR -> " + ex.getMessage());
        }
    }
}
