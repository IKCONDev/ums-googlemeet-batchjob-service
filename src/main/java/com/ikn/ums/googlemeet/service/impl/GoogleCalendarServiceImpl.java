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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikn.ums.googlemeet.dto.ConferenceRecordDto;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingParticipantDto;
import com.ikn.ums.googlemeet.dto.GoogleMeetingDetailsDto;
import com.ikn.ums.googlemeet.dto.GoogleRecurringInstanceDto;
import com.ikn.ums.googlemeet.dto.GoogleRecurringMeetingDetailsDto;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.dto.TranscriptDto;
import com.ikn.ums.googlemeet.model.AccessTokenResponseModel;
import com.ikn.ums.googlemeet.model.GoogleCompletedMeetingParticipantsResponse;
import com.ikn.ums.googlemeet.model.GoogleCompletedMeetingResponse;
import com.ikn.ums.googlemeet.model.GoogleConferenceRecordsResponse;
import com.ikn.ums.googlemeet.model.GoogleRecurringInstancesResponse;
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
            String url = googleUrlFactory.buildRecurringDetailsUrl("userEmail", recurringEventId);

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
    public List<GoogleRecurringInstanceDto> fetchRecurringInstances(String masterEventId) {

        final String method = "fetchRecurringInstances";
        log.info("{} - Fetching instances for recurringEventId={}", method, masterEventId);

        try {
            String url = googleUrlFactory.buildRecurringOccurrencesUrl("primary", masterEventId);
            log.debug("{} - URL={}", method, url);

            ResponseEntity<GoogleRecurringInstancesResponse> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(getHeaders()),
                            GoogleRecurringInstancesResponse.class
                    );

            GoogleRecurringInstancesResponse body = response.getBody();

            if (body == null || body.getItems() == null || body.getItems().isEmpty()) {
                log.warn("{} - No instances found for recurringEventId={}", method, masterEventId);
                return Collections.emptyList();
            }

            log.info("{} - Retrieved {} instances for recurringEventId={}",
                    method, body.getItems().size(), masterEventId);

            return body.getItems();

        } catch (Exception ex) {
            log.error("{} - Error fetching instances for recurringEventId={}",
                    method, masterEventId, ex);
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

            // 1️⃣ Fetch RAW JSON as String
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(getHeaders()),
                    String.class
            );

            String rawJson = rawResponse.getBody();

            // 2️⃣ Log full raw response
            log.info("RAW Participants API response for conferenceRecordId={}: \n{}",
                    conferenceRecordId, rawJson);

            if (rawJson == null || rawJson.isBlank()) {
                return Collections.emptyList();
            }

            // 3️⃣ Manually deserialize
            ObjectMapper objectMapper = new ObjectMapper();
            GoogleCompletedMeetingParticipantsResponse parsed =
                    objectMapper.readValue(rawJson, GoogleCompletedMeetingParticipantsResponse.class);

            return parsed.getParticipants() != null
                    ? parsed.getParticipants()
                    : Collections.emptyList();

        } catch (Exception ex) {
            log.error("Error fetching participants for conferenceRecordId {}",
                    conferenceRecordId, ex);
            return Collections.emptyList();
        }
    }

    public List<TranscriptDto> fetchTranscripts(String conferenceRecordId) {
        final String method = "fetchTranscripts";
        try {
            String url = googleUrlFactory.buildConferenceTranscriptsUrl(conferenceRecordId);
            log.info("{} - Fetching transcripts for conferenceRecordId={} using URL={}", method, conferenceRecordId, url);

            ResponseEntity<TranscriptResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(getHeaders()),
                    TranscriptResponse.class
            );

            if (response.getBody() == null) {
                log.warn("{} - No response body found for conferenceRecordId={}", method, conferenceRecordId);
                return Collections.emptyList();
            }

            List<TranscriptDto> transcripts = response.getBody().getTranscripts();
            if (transcripts == null || transcripts.isEmpty()) {
                log.warn("{} - No transcripts found for conferenceRecordId={}", method, conferenceRecordId);
                return Collections.emptyList();
            }

            log.info("{} - Fetched {} transcripts for conferenceRecordId={}", method, transcripts.size(), conferenceRecordId);

            // Optional: log each transcript ID and docsDestination
            for (TranscriptDto transcript : transcripts) {
                String docId = transcript.getDocsDestination() != null
                        ? transcript.getDocsDestination().getDocument()
                        : "null";
                log.info("{} - Transcript name={}, docsDestination={}", method, transcript.getName(), docId);
            }

            return transcripts;

        } catch (Exception ex) {
            log.error("{} - Error fetching transcripts for conferenceRecordId={}", method, conferenceRecordId, ex);
            return Collections.emptyList();
        }
    }

    
    
    @Override
    public String fetchPlainTranscriptText(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            log.warn("DocumentId is null or empty. Cannot fetch transcript.");
            return "";
        }

        log.info("fetchPlainTranscriptText - Fetching plain transcript for documentId {}", documentId);

        try {
            String url = googleUrlFactory.buildPlainTranscriptExportUrl(documentId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(googleOAuth.getAccessTokenString());
            headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN));

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            String body = response.getBody();

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("fetchPlainTranscriptText - Successfully fetched transcript for documentId {}", documentId);
                log.info("fetchPlainTranscriptText - Full Response Body:\n{}", body); 
            } else {
                log.warn("fetchPlainTranscriptText - Received non-success response {} for documentId {}", response.getStatusCode(), documentId);
            }

            return body;

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("fetchPlainTranscriptText - File not found for documentId {}. Returning empty transcript.", documentId);
            return "";
        } catch (HttpClientErrorException.Forbidden e) {
            log.error("fetchPlainTranscriptText - Access denied for documentId {}. Check permissions.", documentId);
            return "";
        } catch (Exception ex) {
            log.error("fetchPlainTranscriptText - Error fetching plain transcript for documentId {}: {}", documentId, ex.getMessage(), ex);
            return "";
        }
    }


   
}
