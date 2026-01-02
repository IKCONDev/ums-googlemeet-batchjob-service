package com.ikn.ums.googlemeet.service.impl;
 
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Semaphore;
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
import com.ikn.ums.googlemeet.exception.GoogleUserFailedException;
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
    
    
    private static final Semaphore GOOGLE_RATE_LIMITER = new Semaphore(2, true);

    
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
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000, multiplier = 2)
    )
    public List<GoogleScheduledMeetingDto> fetchScheduledMeetings(String userEmail) {
        return fetchScheduledMeetings(userEmail, 1);
    }
    
    /**
     * Private method that performs:
     *   - actual Google API call
     *   - 429 rate limit handling
     *
     * @param userEmail Google user email
     * @param attempt   current retry attempt number
     */
    private List<GoogleScheduledMeetingDto> fetchScheduledMeetings(
            String userEmail, int attempt) {

        final String methodName = "fetchScheduledMeetings()";
        log.info("{} - Attempt {} for {}", methodName, attempt, userEmail);

        if (attempt > MAX_API_RETRIES) {
            throw new GoogleUserFailedException(
                    "MAX_RETRIES_EXCEEDED",
                    "Max retry limit reached for user " + userEmail
            );
        }

        try {
            GOOGLE_RATE_LIMITER.acquire();

            String url = googleUrlFactory.buildUpcomingMeetingsUrl(userEmail);
            log.debug("{} - URL={}", methodName, url);

            ResponseEntity<GoogleScheduledMeetingResponse> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(getHeaders()),
                            GoogleScheduledMeetingResponse.class
                    );

            GoogleScheduledMeetingResponse body = response.getBody();

            if (body == null || body.getItems() == null) {
                log.info("{} - Empty scheduled meetings response for {}",
                        methodName, userEmail);
                return Collections.emptyList();
            }

            log.info("{} - SUCCESS: Retrieved {} scheduled meetings for userEmail={}",
                    methodName, body.getItems().size(), userEmail);

            return body.getItems();

        }
        catch (HttpClientErrorException.NotFound ex) {

            throw new GoogleUserFailedException(
                    "GOOGLE_USER_NOT_FOUND",
                    "Google user does not exist: " + userEmail
            );

        }
        catch (HttpClientErrorException.TooManyRequests ex) {

            int wait = Optional.ofNullable(
                            ex.getResponseHeaders().getFirst("Retry-After"))
                    .map(Integer::parseInt)
                    .orElse(10);

            log.warn("{} - 429 RATE LIMITED for {}. Retrying in {} sec",
                    methodName, userEmail, wait);

            try {
                Thread.sleep(wait * 1000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new GoogleUserFailedException(
                        "THREAD_INTERRUPTED",
                        "Retry interrupted for user " + userEmail
                );
            }

            return fetchScheduledMeetings(userEmail, attempt + 1);

        }
        catch (ResourceAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {

            log.error("{} - Unexpected error for {}",
                    methodName, userEmail, ex);

            throw new GoogleUserFailedException(
                    "FETCH_SCHEDULED_FAILED",
                    "Failed to fetch scheduled meetings for user " + userEmail
            );

        }
        finally {
            try {
                GOOGLE_RATE_LIMITER.release();
            } catch (Exception ex) {
                log.warn("{} -> Failed to release rate limiter",
                        methodName, ex);
            }
        }
    }

    @Override
    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000, multiplier = 2)
    )
    public List<GoogleCompletedMeetingDto> fetchCompletedMeetings(String userEmail) {
        return fetchCompletedMeetings(userEmail, 1);
    }

    /**
     * Private recursive method that:
     *  - Calls the Google "completed meetings" API
     *  - Handles HTTP 429 rate limits
     *
     * @param userEmail Google user email
     * @param attempt retry attempt number
     */
    private List<GoogleCompletedMeetingDto> fetchCompletedMeetings(
            String userEmail, int attempt) {

        final String methodName = "fetchCompletedMeetings()";
        log.info("{} - Attempt {} for {}", methodName, attempt, userEmail);

        if (attempt > MAX_API_RETRIES) {
            throw new GoogleUserFailedException(
                    "MAX_RETRIES_EXCEEDED",
                    "Max retry limit reached for user " + userEmail
            );
        }

        try {
            GOOGLE_RATE_LIMITER.acquire();

           
            String url = googleUrlFactory.buildCompletedMeetingsUrl(userEmail);

          
            url += "&singleEvents=true&orderBy=startTime";

            log.debug("{} - URL={}", methodName, url);

            ResponseEntity<GoogleCompletedMeetingResponse> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(getHeaders()),
                            GoogleCompletedMeetingResponse.class
                    );

            GoogleCompletedMeetingResponse body = response.getBody();

            if (body == null || body.getItems() == null) {
                log.info("{} - Empty completed meetings response for {}",
                        methodName, userEmail);
                return Collections.emptyList();
            }

            List<GoogleCompletedMeetingDto> meetings = body.getItems();

            log.info("{} - SUCCESS: Retrieved {} completed meetings for userEmail={}",
                    methodName, meetings.size(), userEmail);

            return meetings;

        }
        catch (HttpClientErrorException.NotFound ex) {
            throw new GoogleUserFailedException(
                    "GOOGLE_USER_NOT_FOUND",
                    "Google user does not exist: " + userEmail
            );
        }
        catch (HttpClientErrorException.TooManyRequests ex) {

            int wait = Optional.ofNullable(
                            ex.getResponseHeaders().getFirst("Retry-After"))
                    .map(Integer::parseInt)
                    .orElse(10);

            log.warn("{} - 429 RATE LIMITED for {}. Retrying in {} sec",
                    methodName, userEmail, wait);

            try {
                Thread.sleep(wait * 1000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new GoogleUserFailedException(
                        "THREAD_INTERRUPTED",
                        "Retry interrupted for user " + userEmail
                );
            }

            return fetchCompletedMeetings(userEmail, attempt + 1);

        }
        catch (ResourceAccessException ex) {
            throw ex; 
        }
        catch (Exception ex) {
            throw new GoogleUserFailedException(
                    "FETCH_COMPLETED_FAILED",
                    "Failed to fetch completed meetings for user " + userEmail
            );
        }
        finally {
            try {
                GOOGLE_RATE_LIMITER.release();
            } catch (Exception ex) {
                log.warn("{} -> Failed to release rate limiter", methodName, ex);
            }
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

        final String method = "fetchInvitees()";

        try {

            if (eventId == null || eventId.isBlank()) {
                log.warn("{} -> Invalid eventId", method);
                return Collections.emptyList();
            }

            if (attendeeType == null) {
                log.warn("{} -> attendeeType is null", method);
                return Collections.emptyList();
            }

            log.info("{} -> Fetching invitees for eventId={}, targetType={}",
                    method, eventId, attendeeType.getSimpleName());

            GOOGLE_RATE_LIMITER.acquire();

            GoogleMeetingDetailsDto event = fetchMeetingDetails(eventId);

            if (event == null || event.getAttendees() == null || event.getAttendees().isEmpty()) {
                log.info("{} -> No invitees found for eventId={}", method, eventId);
                return Collections.emptyList();
            }

            List<?> attendees = event.getAttendees();

            log.info("{} -> Retrieved {} invitees for eventId={}",
                    method, attendees.size(), eventId);

            return attendees.stream()
                    .map(item -> {
                        try {
                            return modelMapper.map(item, attendeeType);
                        } catch (Exception mapEx) {
                            log.error("{} -> Mapping failed: {}", method, mapEx.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception ex) {

            log.error("{} -> ERROR for eventId={} -> {}",
                    method, eventId, ex.getMessage(), ex);

            return Collections.emptyList();

        } finally {
            try {
                GOOGLE_RATE_LIMITER.release();
            } catch (Exception ex) {
                log.warn("{} -> Failed to release rate limiter: {}", method, ex.getMessage());
            }
        }
    }


    
    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000, multiplier = 2)
    )
    @Override
    public GoogleRecurringMeetingDetailsDto fetchRecurringMeetingDetails(String recurringEventId) {

        final String methodName = "fetchRecurringMeetingDetails()";
        log.info("{} - Fetching occurrences for recurringEventId={}", methodName, recurringEventId);

        try {

            GOOGLE_RATE_LIMITER.acquire();

            String url = googleUrlFactory.buildRecurringDetailsUrl("userEmail", recurringEventId);
            log.debug("{} - URL={}", methodName, url);

            ResponseEntity<GoogleRecurringMeetingDetailsDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(getHeaders()),
                            GoogleRecurringMeetingDetailsDto.class
                    );

            GoogleRecurringMeetingDetailsDto details = response.getBody();

            if (details == null) {
                log.warn("{} - No meeting details returned for recurringEventId={}", methodName, recurringEventId);
                return null;
            }

            return details;

        } catch (HttpClientErrorException.TooManyRequests ex) {
            // Handle 429
            int wait = Optional.ofNullable(ex.getResponseHeaders().getFirst("Retry-After"))
                    .map(Integer::parseInt)
                    .orElse(10);

            log.warn("{} - 429 RATE LIMIT for recurringEventId={}. Retrying in {} sec",
                    methodName, recurringEventId, wait);

            try {
                Thread.sleep(wait * 1000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            return fetchRecurringMeetingDetails(recurringEventId); // retry

        } catch (Exception ex) {
            log.error("{} - ERROR fetching occurrences for recurringEventId={} → {}",
                    methodName, recurringEventId, ex.getMessage());
            return null;
        } finally {
            try {
                GOOGLE_RATE_LIMITER.release();
            } catch (Exception ex) {
                log.warn("{} -> Failed to release rate limiter: {}", methodName, ex.getMessage());
            }
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


    @Retryable(
            retryFor = {ResourceAccessException.class, IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000, multiplier = 2)
    )
    @Override
    public List<GoogleCompletedMeetingParticipantDto> fetchParticipants(String conferenceRecordId) {
        final String method = "fetchParticipants()";
        log.info("{} - Fetching participants for conferenceRecordId={}", method, conferenceRecordId);

        try {

            GOOGLE_RATE_LIMITER.acquire();

            String url = googleUrlFactory.buildConferenceParticipantsUrl(conferenceRecordId);
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(getHeaders()),
                    String.class
            );

            String rawJson = rawResponse.getBody();
            log.info("{} - RAW Participants API response for conferenceRecordId={}: \n{}",
                    method, conferenceRecordId, rawJson);

            if (rawJson == null || rawJson.isBlank()) {
                log.warn("{} - No participants returned for conferenceRecordId={}", method, conferenceRecordId);
                return Collections.emptyList();
            }

            ObjectMapper objectMapper = new ObjectMapper();
            GoogleCompletedMeetingParticipantsResponse parsed =
                    objectMapper.readValue(rawJson, GoogleCompletedMeetingParticipantsResponse.class);

            List<GoogleCompletedMeetingParticipantDto> participants =
                    parsed.getParticipants() != null ? parsed.getParticipants() : Collections.emptyList();

            log.info("{} - Retrieved {} participants for conferenceRecordId={}", method, participants.size(), conferenceRecordId);

            return participants;

        } catch (HttpClientErrorException.TooManyRequests ex) {
            int wait = Optional.ofNullable(ex.getResponseHeaders().getFirst("Retry-After"))
                    .map(Integer::parseInt)
                    .orElse(10);

            log.warn("{} - 429 RATE LIMIT for conferenceRecordId={}. Retrying in {} sec",
                    method, conferenceRecordId, wait);

            try { Thread.sleep(wait * 1000L); } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            return fetchParticipants(conferenceRecordId); // retry

        } catch (Exception ex) {
            log.error("{} - Error fetching participants for conferenceRecordId {}: {}", method, conferenceRecordId, ex.getMessage(), ex);
            return Collections.emptyList();
        } finally {
            try {
                GOOGLE_RATE_LIMITER.release();
            } catch (Exception ex) {
                log.warn("{} -> Failed to release rate limiter: {}", method, ex.getMessage());
            }
        }
    }

    @Retryable(
            retryFor = {ResourceAccessException.class, IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000, multiplier = 2)
    )
    @Override
    public List<TranscriptDto> fetchTranscripts(String conferenceRecordId) {
        final String method = "fetchTranscriptsWithRateLimit";
        log.info("{} - Fetching transcripts for conferenceRecordId={}", method, conferenceRecordId);

        try {

            GOOGLE_RATE_LIMITER.acquire();

            String url = googleUrlFactory.buildConferenceTranscriptsUrl(conferenceRecordId);
            log.info("{} - Using URL={}", method, url);

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

            for (TranscriptDto transcript : transcripts) {
                String docId = transcript.getDocsDestination() != null
                        ? transcript.getDocsDestination().getDocument()
                        : "null";
                log.info("{} - Transcript name={}, docsDestination={}", method, transcript.getName(), docId);
            }

            return transcripts;

        } catch (HttpClientErrorException.TooManyRequests ex) {

            int wait = Optional.ofNullable(ex.getResponseHeaders().getFirst("Retry-After"))
                    .map(Integer::parseInt)
                    .orElse(10);

            log.warn("{} - 429 RATE LIMIT for conferenceRecordId={}. Retrying in {} sec...", method, conferenceRecordId, wait);

            try { Thread.sleep(wait * 1000L); } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            return fetchTranscripts(conferenceRecordId); 

        } catch (Exception ex) {
            log.error("{} - Error fetching transcripts for conferenceRecordId={}", method, conferenceRecordId, ex);
            return Collections.emptyList();
        } finally {
            try {
                GOOGLE_RATE_LIMITER.release();
            } catch (Exception ex) {
                log.warn("{} -> Failed to release rate limiter: {}", method, ex.getMessage());
            }
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
