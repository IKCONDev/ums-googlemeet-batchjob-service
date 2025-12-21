package com.ikn.ums.googlemeet.service.impl;
 
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
 
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingParticipantDto;
import com.ikn.ums.googlemeet.dto.TranscriptDto;
import com.ikn.ums.googlemeet.entity.GoogleCompletedMeeting;
import com.ikn.ums.googlemeet.entity.GoogleCompletedMeetingParticipant;
import com.ikn.ums.googlemeet.entity.GoogleMeetTranscriptEntity;
import com.ikn.ums.googlemeet.enums.GoogleMeetingType;
import com.ikn.ums.googlemeet.model.GoogleCompletedMeetingResponse;
import com.ikn.ums.googlemeet.processor.GoogleCompletedMeetingProcessor;
import com.ikn.ums.googlemeet.repo.GoogleCompletedMeetingRepository;
import com.ikn.ums.googlemeet.service.GoogleCompletedMeetingService;
import com.ikn.ums.googlemeet.utils.InitializeGoogleOAuth;
 
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
 
@Service
@Slf4j
public class GoogleCompletedMeetingServiceImpl implements GoogleCompletedMeetingService {
 
    @Autowired
    private InitializeGoogleOAuth initializeGoogleOAuth;
 
    @Autowired
    private GoogleCompletedMeetingRepository googleMeetingRepository;
 
    @Autowired
    @Qualifier("googleRestTemplate")
    private RestTemplate restTemplate;
    
    @Autowired
    private GoogleCompletedMeetingProcessor meetingProcessor;
 
 
    @Override
    public List<GoogleCompletedMeetingDto> performMeetingsRawDataBatchProcessing() {
 
        List<String> emailIds = List.of("ums-test@ikcontech.com");
 
        List<CompletableFuture<List<GoogleCompletedMeetingDto>>> futures =
                emailIds.stream()
                        .map(this::getEventsAsync)
                        .toList();
 
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
       
        List<GoogleCompletedMeetingDto> completedMeetings =
                futures.stream()
                        .flatMap(f -> f.join().stream())
                        .toList();
 
        // Delegate business logic to processor
        completedMeetings = meetingProcessor.preProcess(completedMeetings);
        completedMeetings = completedMeetings.stream()
                .map(meetingProcessor::attachConferenceData)
                .toList();
 
 
        List<GoogleCompletedMeeting> entities =
                completedMeetings.stream()
                        .map(this::mapToEntity)
                        .toList();
 
        persistCompletedMeetings(entities);
 
        log.info("Completed meetings saved/updated: {}", entities.size());
 
        return completedMeetings;
    }
 
    private GoogleCompletedMeeting mapToEntity(GoogleCompletedMeetingDto dto) {
 
        GoogleCompletedMeeting meeting = new GoogleCompletedMeeting();
 
        meeting.setGoogleEventId(dto.getId());
        meeting.setSummary(dto.getSummary());
        meeting.setDescription(dto.getDescription());
        meeting.setHangoutLink(dto.getHangoutLink());
        meeting.setStartTime(dto.getStartTime());
        meeting.setEndTime(dto.getEndTime());
 
      
        meeting.setMeetingType(
                GoogleMeetingType.valueOf(dto.getMeetingType())
        );
        
     // Participants
        if (dto.getParticipants() != null) {
            dto.getParticipants().forEach(p -> {
                GoogleCompletedMeetingParticipant entity =
                        mapParticipant(p);
                entity.setMeeting(meeting);
                meeting.getParticipants().add(entity);
            });
        }

        // Transcripts
        if (dto.getTranscripts() != null) {
            dto.getTranscripts().forEach(t -> {
                GoogleMeetTranscriptEntity entity =
                        mapTranscript(t);
                entity.setMeeting(meeting);
                meeting.getTranscripts().add(entity);
            });
        }

 
        return meeting;
    }
 
    @Transactional
    private void persistCompletedMeetings(List<GoogleCompletedMeeting> meetings) {
 
        for (GoogleCompletedMeeting meeting : meetings) {
 
            GoogleCompletedMeeting existing =
                    googleMeetingRepository
                            .findByGoogleEventId(meeting.getGoogleEventId())
                            .orElse(null);
 
            if (existing == null) {
                // INSERT
                googleMeetingRepository.save(meeting);
            } else {
                // UPDATE
                existing.setMeetingType(meeting.getMeetingType());
                existing.setSummary(meeting.getSummary());
                existing.setDescription(meeting.getDescription());
                existing.setStartTime(meeting.getStartTime());
                existing.setEndTime(meeting.getEndTime());
                existing.setHangoutLink(meeting.getHangoutLink());
                
                existing.getParticipants().clear();
                existing.getTranscripts().clear();

                // Add new children
                meeting.getParticipants().forEach(p -> p.setMeeting(existing));
                meeting.getTranscripts().forEach(t -> t.setMeeting(existing));

                existing.getParticipants().addAll(meeting.getParticipants());
                existing.getTranscripts().addAll(meeting.getTranscripts());
 
                googleMeetingRepository.save(existing);
            }
        }
    }
 
 
    @Async("googleMeetExecutor")
    public CompletableFuture<List<GoogleCompletedMeetingDto>> getEventsAsync(String userEmail) {
        return CompletableFuture.completedFuture(getEvents(userEmail));
    }
 
    private List<GoogleCompletedMeetingDto> getEvents(String userEmail) {
 
        try {
            String token = initializeGoogleOAuth.getAccessTokenString();
 
            String url = initializeGoogleOAuth.getBaseUrl()
                    + "/calendars/" + userEmail
                    + "/events?conferenceDataVersion=1";
 
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
 
            ResponseEntity<GoogleCompletedMeetingResponse> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            GoogleCompletedMeetingResponse.class
                    );
 
            return response.getBody() != null
                    ? response.getBody().getItems()
                    : Collections.emptyList();
 
        } catch (Exception e) {
            log.error("Error fetching events for {}", userEmail, e);
            return Collections.emptyList();
        }
    }
    
    private GoogleCompletedMeetingParticipant mapParticipant(
            GoogleCompletedMeetingParticipantDto dto) {

        GoogleCompletedMeetingParticipant entity =
                new GoogleCompletedMeetingParticipant();

        entity.setName(dto.getName());

        if (dto.getSignedinUser() != null) {
            entity.setUserId(dto.getSignedinUser().getUser());
            entity.setDisplayName(dto.getSignedinUser().getDisplayName());
        }

        if (dto.getEarliestStartTime() != null) {
            entity.setEarliestStartTime(
                    OffsetDateTime.parse(dto.getEarliestStartTime())
            );
        }

        if (dto.getLatestEndTime() != null) {
            entity.setLatestEndTime(
                    OffsetDateTime.parse(dto.getLatestEndTime())
            );
        }

        return entity;
    }

    
    private GoogleMeetTranscriptEntity mapTranscript(TranscriptDto dto) {

        GoogleMeetTranscriptEntity entity =
                new GoogleMeetTranscriptEntity();

        entity.setName(dto.getName());
        entity.setState(dto.getState());
        if (dto.getStartTime() != null) {
            entity.setStartTime(dto.getStartTime().toOffsetDateTime());
        }

        if (dto.getEndTime() != null) {
            entity.setEndTime(dto.getEndTime().toOffsetDateTime());
        }

        if (dto.getDocsDestination() != null) {
            entity.setDocument(dto.getDocsDestination().getDocument());
            entity.setExportUri(dto.getDocsDestination().getExportUri());
        }

        return entity;
    }

}
 
 