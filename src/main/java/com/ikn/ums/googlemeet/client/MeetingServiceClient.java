package com.ikn.ums.googlemeet.client;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.ikn.ums.googlemeet.externaldto.UMSCompletedMeetingDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MeetingServiceClient {

    @Autowired
    @Qualifier("internalEurekaClientRestTemplate")
    private RestTemplate restTemplate;

    private static final String CREATE_MEETINGS_URL =
            "http://UMS-MEETING-SERVICE/meetings/create";


    /**
     * Sends the current batch completed meetings to the Meeting Service (UMS-MEETING-SERVICE) and returns
     * the list of meetings persisted in that service.
     */
    @Retry(name = "meetingServiceRetry", fallbackMethod = "syncCompletedMeetingsFallback")
    @CircuitBreaker(name = "meetingServiceCircuit", fallbackMethod = "syncCompletedMeetingsFallback")
    public List<UMSCompletedMeetingDto> syncCompletedMeetingsToMeetingService(
            List<UMSCompletedMeetingDto> meetings) {

        final String method = "syncCompletedMeetingsToMeetingService()";
        log.info("{} - Sending {} completed meetings to Meeting Service...",
                method, meetings.size());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<UMSCompletedMeetingDto>> requestEntity =
                new HttpEntity<>(meetings, headers);

        ResponseEntity<List<UMSCompletedMeetingDto>> response =
                restTemplate.exchange(
                        CREATE_MEETINGS_URL,
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<List<UMSCompletedMeetingDto>>() {}
                );

        List<UMSCompletedMeetingDto> savedMeetings = response.getBody();

        if (savedMeetings == null || savedMeetings.isEmpty()) {
            log.warn("{} - Meeting Service returned empty list", method);
            return Collections.emptyList();
        }

        log.info("{} - Meeting Service saved {} meetings", method, savedMeetings.size());
        return savedMeetings;
    }


    /**
     * Fallback when Meeting Service is unavailable.
     */
    public List<UMSCompletedMeetingDto> syncCompletedMeetingsFallback(
            List<UMSCompletedMeetingDto> meetings, Throwable ex) {

        final String method = "syncCompletedMeetingsFallback()";
        log.error("{} - Meeting Service DOWN. Reason: {}", method, ex.toString());

        log.warn("{} - Returning empty list (no cache for meetings).", method);
        return Collections.emptyList();
    }
}
