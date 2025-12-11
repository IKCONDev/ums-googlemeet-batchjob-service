package com.ikn.ums.googlemeet.service.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.dto.GoogleMeetEventsResponse;
import com.ikn.ums.googlemeet.entity.GoogleCompletedMeeting;
import com.ikn.ums.googlemeet.repo.GoogleCompletedMeetingRepository;
import com.ikn.ums.googlemeet.service.GoogleCompletedMeetingService;
import com.ikn.ums.googlemeet.utils.InitializeGoogleOAuth;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GoogleCompletedMeetingServiceImpl implements GoogleCompletedMeetingService {

    @Autowired
    private InitializeGoogleOAuth initializeGoogleOAuth;

    @Autowired
    private GoogleCompletedMeetingRepository googleMeetingRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    @Qualifier("googleRestTemplate")
    private RestTemplate restTemplate;

    @Override
    public List<GoogleCompletedMeetingDto> performMeetingsRawDataBatchProcessing() {
        String methodName = "performMeetingsRawDataBatchProcessing()";
        log.info("{} - STARTED", methodName);

        List<String> emailIds = List.of("siri.chenimini1@gmail.com");
        List<CompletableFuture<List<GoogleCompletedMeetingDto>>> futuresList = new ArrayList<>();

        for (String email : emailIds) {
            futuresList.add(getEventsAsync(email));
        }

        CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[0])).join();

        // Merge all events
        List<GoogleCompletedMeetingDto> allEvents = futuresList.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                // Filter only completed meetings
                .filter(e -> {
                    if (e.getEndTime() == null) return false;
                    try {
                        OffsetDateTime endTime = OffsetDateTime.parse(e.getEndTime());
                        return endTime.isBefore(OffsetDateTime.now(ZoneOffset.UTC));
                    } catch (Exception ex) {
                        log.warn("Failed to parse endTime '{}': {}", e.getEndTime(), ex.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.toList());

        // Map DTO to Entity manually
        List<GoogleCompletedMeeting> meetingsToSave = allEvents.stream()
                .map(dto -> {
                    GoogleCompletedMeeting meeting = new GoogleCompletedMeeting();
                    meeting.setGoogleEventId(dto.getId());
                    meeting.setSummary(dto.getSummary());
                    meeting.setDescription(dto.getDescription());
                    meeting.setHangoutLink(dto.getHangoutLink());
                    meeting.setStartTime(dto.getStartTime());
                    meeting.setEndTime(dto.getEndTime());
                    return meeting;
                })
                .collect(Collectors.toList());

        persistCompletedMeetings(meetingsToSave);

        log.info("{} - COMPLETED. Total completed meetings saved: {}", methodName, meetingsToSave.size());
        return allEvents;
    }

    @Transactional(value = TxType.REQUIRED)
    private void persistCompletedMeetings(List<GoogleCompletedMeeting> meetings) {
        for (GoogleCompletedMeeting meeting : meetings) {
            if (!googleMeetingRepository.existsBygoogleEventId(meeting.getGoogleEventId())) {
                googleMeetingRepository.save(meeting);
            }
        }
    }

    @Async("googleMeetExecutor")
    public CompletableFuture<List<GoogleCompletedMeetingDto>> getEventsAsync(String userEmail) {
        List<GoogleCompletedMeetingDto> events = getEvents(userEmail);
        return CompletableFuture.completedFuture(events);
    }

    private List<GoogleCompletedMeetingDto> getEvents(String userEmail) {
        String methodName = "getEvents()";

        try {
            String accessToken = initializeGoogleOAuth.getAccessTokenString();
            if (accessToken == null) {
                log.error("{} - Access token returned null for user {}", methodName, userEmail);
                return Collections.emptyList();
            }

            String url = initializeGoogleOAuth.getBaseUrl()
                    + "/calendars/" + userEmail + "/events?conferenceDataVersion=1";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<GoogleMeetEventsResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, GoogleMeetEventsResponse.class);

            List<GoogleCompletedMeetingDto> events = response.getBody() != null ? response.getBody().getItems() : Collections.emptyList();
            log.info("{} - Fetched {} events for user {}", methodName, events.size(), userEmail);

            return events;

        } catch (Exception ex) {
            log.error("{} - Error fetching events for user {}: {}", methodName, userEmail, ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }
}
