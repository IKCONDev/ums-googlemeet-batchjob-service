package com.ikn.ums.googlemeet.service.impl;
 
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
 
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingAttendeeDto;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingAttendeeDto;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.entity.GoogleCompletedMeetingAttendee;
import com.ikn.ums.googlemeet.entity.GoogleScheduledMeeting;
import com.ikn.ums.googlemeet.entity.GoogleScheduledMeetingAttendee;
import com.ikn.ums.googlemeet.enums.GoogleMeetingType;
import com.ikn.ums.googlemeet.model.GoogleScheduledMeetingResponse;
import com.ikn.ums.googlemeet.processor.GoogleScheduledMeetingProcessor;
import com.ikn.ums.googlemeet.repo.GoogleScheduledMeetingRepository;
import com.ikn.ums.googlemeet.service.GoogleScheduledMeetingService;
import com.ikn.ums.googlemeet.utils.InitializeGoogleOAuth;
 
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
 
@Service
@Slf4j
public class GoogleScheduledMeetingServiceImpl implements GoogleScheduledMeetingService {
 
    @Autowired
    private InitializeGoogleOAuth initializeGoogleOAuth;
 
    @Autowired
    private GoogleScheduledMeetingRepository scheduledMeetingRepository;
 
    @Autowired
    @Qualifier("googleRestTemplate")
    private RestTemplate restTemplate;
 
    @Autowired
    private GoogleScheduledMeetingProcessor meetingProcessor;
 
    @Override
    public List<GoogleScheduledMeetingDto> performScheduledMeetingsRawDataBatchProcessing() {
 
        List<String> emailIds = List.of("ums-test@ikcontech.com");
 
        List<CompletableFuture<List<GoogleScheduledMeetingDto>>> futures =
                emailIds.stream()
                        .map(this::getScheduledMeetingsAsync)
                        .toList();
 
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
 
        List<GoogleScheduledMeetingDto> scheduledMeetings =
                futures.stream()
                        .flatMap(f -> f.join().stream())
                        .toList();
 
        // Delegate business logic to processor
        scheduledMeetings = meetingProcessor.preProcess(scheduledMeetings);
 
        List<GoogleScheduledMeeting> entities =
                scheduledMeetings.stream()
                        .map(this::mapToEntity)
                        .toList();
 
        persistScheduledMeetings(entities);
 
        log.info("Scheduled meetings saved/updated: {}", entities.size());
 
        return scheduledMeetings;
    }
 
    private GoogleScheduledMeeting mapToEntity(GoogleScheduledMeetingDto dto) {
        GoogleScheduledMeeting meeting = new GoogleScheduledMeeting();
 
        meeting.setGoogleEventId(dto.getId());
        meeting.setSummary(dto.getSummary());
        meeting.setDescription(dto.getDescription());
        meeting.setHangoutLink(dto.getHangoutLink());
        meeting.setStartTime(dto.getStartTime());
        meeting.setEndTime(dto.getEndTime());
        meeting.setMeetingType(
                GoogleMeetingType.valueOf(dto.getMeetingType())
        );
        
        if (dto.getAttendees() != null) {
            for (GoogleScheduledMeetingAttendeeDto aDto : dto.getAttendees()) {
                GoogleScheduledMeetingAttendee attendee = new GoogleScheduledMeetingAttendee();
                attendee.setEmail(aDto.getEmail());
                attendee.setOrganizer(aDto.getOrganizer());
                attendee.setSelf(aDto.getSelf());
                attendee.setResponseStatus(aDto.getResponseStatus());
 
                meeting.addAttendee(attendee);
            }
        }
 
 
        return meeting;
    }
 
    @Transactional
    private void persistScheduledMeetings(List<GoogleScheduledMeeting> meetings) {
 
        for (GoogleScheduledMeeting meeting : meetings) {
 
            GoogleScheduledMeeting existing =
                    scheduledMeetingRepository
                            .findByGoogleEventId(meeting.getGoogleEventId())
                            .orElse(null);
 
            if (existing == null) {
                scheduledMeetingRepository.save(meeting);
            } else {
            	existing.setMeetingType(meeting.getMeetingType());
                existing.setSummary(meeting.getSummary());
                existing.setDescription(meeting.getDescription());
                existing.setStartTime(meeting.getStartTime());
                existing.setEndTime(meeting.getEndTime());
                existing.setHangoutLink(meeting.getHangoutLink());
                
             // Clear and re-add attendees
                existing.getAttendees().clear();
                meeting.getAttendees().forEach(existing::addAttendee);
 
             
                scheduledMeetingRepository.save(existing);
            }
        }
    }
 
    @Async("googleMeetExecutor")
    public CompletableFuture<List<GoogleScheduledMeetingDto>> getScheduledMeetingsAsync(String userEmail) {
        return CompletableFuture.completedFuture(getScheduledMeetings(userEmail));
    }
 
    private List<GoogleScheduledMeetingDto> getScheduledMeetings(String userEmail) {
        try {
            String token = initializeGoogleOAuth.getAccessTokenString();
 
            String url = initializeGoogleOAuth.getBaseUrl()
                    + "/calendars/" + userEmail
                    + "/events?conferenceDataVersion=1";
 
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
 
            ResponseEntity<GoogleScheduledMeetingResponse> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            GoogleScheduledMeetingResponse.class
                    );
 
            return response.getBody() != null
                    ? response.getBody().getItems()
                    : Collections.emptyList();
 
        } catch (Exception e) {
            log.error("Error fetching scheduled meetings for {}", userEmail, e);
            return Collections.emptyList();
        }
    }
 
}
 
 