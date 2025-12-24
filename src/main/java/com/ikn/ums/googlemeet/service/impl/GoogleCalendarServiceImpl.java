package com.ikn.ums.googlemeet.service.impl;
 
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
 
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.ikn.ums.googlemeet.dto.ConferenceRecordDto;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingParticipantDto;
import com.ikn.ums.googlemeet.dto.GoogleMeetingDetailsDto;
import com.ikn.ums.googlemeet.dto.GoogleRecurringMeetingDetailsDto;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.dto.TranscriptDto;
import com.ikn.ums.googlemeet.model.AccessTokenResponseModel;
import com.ikn.ums.googlemeet.model.GoogleCompletedMeetingParticipantsResponse;
import com.ikn.ums.googlemeet.model.GoogleCompletedMeetingResponse;
import com.ikn.ums.googlemeet.model.GoogleConferenceRecordsResponse;
import com.ikn.ums.googlemeet.model.GoogleScheduledMeetingResponse;
import com.ikn.ums.googlemeet.model.TranscriptResponse;
import com.ikn.ums.googlemeet.service.GoogleCalendarService;
import com.ikn.ums.googlemeet.utils.GoogleUrlFactory;
import com.ikn.ums.googlemeet.utils.InitializeGoogleOAuth;
 
import lombok.extern.slf4j.Slf4j;
 
 
/**
* Google Calendar service impl
*
* This service is responsible for all outbound communication with
* Google APIs such as:
*   - Fetch scheduled meetings
*   - Fetch completed meetings
*   - Fetch meeting invitees
*   - Fetch meeting details
*
* Responsibilities:
*   - Build Google API URLs dynamically
*   - Authenticate using Google OAuth access token
*   - Execute RestTemplate calls
*   - Handle 429 rate limits (Retry-After logic)
*   - Apply Spring Retry for network failures
*   - Fail safely and return empty lists instead of throwing errors
*/
 
@Service
@Slf4j
public class GoogleCalendarServiceImpl implements GoogleCalendarService {

    private static final int MAX_API_RETRIES = 3;

    @Autowired
    @Qualifier("externalIntegrationRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private InitializeGoogleOAuth googleOAuth;

    @Autowired
    private GoogleUrlFactory googleUrlFactory;

    @Autowired
    private ModelMapper modelMapper;

    
    @Override
    public AccessTokenResponseModel getAccessToken() {
        return googleOAuth.getAccessToken();
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(googleOAuth.getAccessTokenString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    
    @Override
    @Retryable(
        retryFor = { ResourceAccessException.class, IOException.class },
        maxAttempts = MAX_API_RETRIES,
        backoff = @Backoff(delay = 3000)
    )
    public List<GoogleScheduledMeetingDto> fetchScheduledMeetings(String userEmail) {

        try {
            String url = googleUrlFactory.buildUpcomingMeetingsUrl(userEmail);

            ResponseEntity<GoogleScheduledMeetingResponse> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(getHeaders()),
                            GoogleScheduledMeetingResponse.class
                    );

            return response.getBody() != null
                    ? response.getBody().getItems()
                    : Collections.emptyList();

        } catch (Exception ex) {
            log.error("fetchScheduledMeetings error", ex);
            return Collections.emptyList();
        }
    }

    @Override
    public List<GoogleCompletedMeetingDto> fetchCompletedMeetings(String userEmail) {
        try {
            // Fetch events from 2 days ago until now
            LocalDate fromDate = LocalDate.now().minusDays(2);

            String url = googleUrlFactory.buildCompletedMeetingsUrl(userEmail);

            // Optional: Add query params directly if your UrlBuilder doesn't handle them
            url += "&singleEvents=true&orderBy=startTime";

            ResponseEntity<GoogleCompletedMeetingResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(getHeaders()),
                    GoogleCompletedMeetingResponse.class
            );

            return response.getBody() != null
                    ? response.getBody().getItems()
                    : Collections.emptyList();

        } catch (Exception ex) {
            log.error("fetchCompletedMeetings error for user " + userEmail, ex);
            return Collections.emptyList();
        }
    }

    
    @Override
    public GoogleMeetingDetailsDto fetchMeetingDetails(String eventId) {

        try {
            String url = googleUrlFactory.buildMeetingDetailsUrl("primary", eventId);

            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(getHeaders()),
                    GoogleMeetingDetailsDto.class
            ).getBody();

        } catch (Exception ex) {
            log.error("fetchMeetingDetails error", ex);
            return null;
        }
    }

    
    @Override
    public <T> List<T> fetchInvitees(String eventId, Class<T> attendeeType) {

        GoogleMeetingDetailsDto event = fetchMeetingDetails(eventId);

        if (event == null || event.getAttendees() == null) {
            return Collections.emptyList();
        }

        return event.getAttendees()
                .stream()
                .map(a -> modelMapper.map(a, attendeeType))
                .collect(Collectors.toList());
    }

    
    @Override
    public GoogleRecurringMeetingDetailsDto fetchRecurringMeetingDetails(String recurringEventId) {

        try {
            String url = googleUrlFactory.buildRecurringDetailsUrl("primary", recurringEventId);

            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(getHeaders()),
                    GoogleRecurringMeetingDetailsDto.class
            ).getBody();

        } catch (Exception ex) {
            log.error("fetchRecurringMeetingDetails error", ex);
            return null;
        }
    }


    @Override
    public List<GoogleScheduledMeetingDto> fetchRecurringInstances(String masterEventId) {

        try {
            String url = googleUrlFactory.buildRecurringOccurrencesUrl("primary", masterEventId);

            ResponseEntity<GoogleScheduledMeetingResponse> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(getHeaders()),
                            GoogleScheduledMeetingResponse.class
                    );

            return response.getBody() != null
                    ? response.getBody().getItems()
                    : Collections.emptyList();

        } catch (Exception ex) {
            log.error("fetchRecurringInstances error", ex);
            return Collections.emptyList();
        }
    }

    @Override
    public List<ConferenceRecordDto> fetchConferenceRecords() {
        try {
            String url = googleUrlFactory.buildConferenceRecordsUrl();

            ResponseEntity<GoogleConferenceRecordsResponse> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(getHeaders()),
                            GoogleConferenceRecordsResponse.class
                    );

            return response.getBody() != null
                    ? response.getBody().getConferenceRecords()
                    : Collections.emptyList();

        } catch (Exception ex) {
            log.error("Error fetching conference records: {}", ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }


    @Override
    public List<GoogleCompletedMeetingParticipantDto> fetchParticipants(String conferenceRecordId) {
        try {
            String url = googleUrlFactory.buildConferenceParticipantsUrl(conferenceRecordId);

            ResponseEntity<GoogleCompletedMeetingParticipantsResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(getHeaders()),
                    GoogleCompletedMeetingParticipantsResponse.class
            );

            return response.getBody() != null
                    ? response.getBody().getParticipants()
                    : Collections.emptyList();

        } catch (Exception ex) {
            log.error("Error fetching participants for conferenceRecordId {}: {}", conferenceRecordId, ex.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<TranscriptDto> fetchTranscripts(String conferenceRecordId) {
        try {
            String url = googleUrlFactory.buildConferenceTranscriptsUrl(conferenceRecordId);

            ResponseEntity<TranscriptResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(getHeaders()),
                    TranscriptResponse.class
            );

            return response.getBody() != null
                    ? response.getBody().getTranscripts()
                    : Collections.emptyList();

        } catch (Exception ex) {
            log.error("Error fetching transcripts for conferenceRecordId {}: {}", conferenceRecordId, ex.getMessage());
            return Collections.emptyList();
        }
    }
    
    
    @Override
    public String fetchPlainTranscriptText(String documentId) {
        try {
            String url = googleUrlFactory.buildPlainTranscriptExportUrl(documentId);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(getHeaders()),
                    String.class
            );

            return response.getBody();

        } catch (Exception ex) {
            log.error("Error fetching plain transcript for documentId {}: {}", documentId, ex.getMessage(), ex);
            return null;
        }
    }



   
}
