package com.ikn.ums.googlemeet.service.impl;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.net.ssl.SSLHandshakeException;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.model.AccessTokenResponseModel;
import com.ikn.ums.googlemeet.model.GoogleCompletedMeetingResponse;
import com.ikn.ums.googlemeet.model.GoogleScheduledMeetingResponse;
import com.ikn.ums.googlemeet.service.GoogleCalendarService;
import com.ikn.ums.googlemeet.utils.GoogleUrlFactory;
import com.ikn.ums.googlemeet.utils.InitializeGoogleOAuth;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
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


    // ---------------------------
    // GET ACCESS TOKEN
    // ---------------------------
    @Override
    public AccessTokenResponseModel getAccessToken() {
        String method = "getAccessToken()";
        log.info("{} - Getting Google Access Token", method);

        AccessTokenResponseModel token = googleOAuth.getAccessToken();
        if (token == null) {
            log.error("{} - FAILED: NULL Google access token", method);
        }
        return token;
    }


    // ---------------------------
    // FETCH SCHEDULED MEETINGS
    // ---------------------------
    @Override
    @Retryable(
        retryFor = {ResourceAccessException.class, IOException.class},
        maxAttempts = MAX_API_RETRIES,
        backoff = @Backoff(delay = 3000, multiplier = 1)
    )
    public List<GoogleScheduledMeetingDto> fetchScheduledMeetings(String userId) {
        return fetchScheduledMeetings(userId, 2);
    }

    private List<GoogleScheduledMeetingDto> fetchScheduledMeetings(String userId, int attempt) {
        String method = "fetchScheduledMeetings()";
        log.info("{} - Attempt {} for {}", method, attempt, userId);

        if (attempt > MAX_API_RETRIES) return Collections.emptyList();

        try {
            AccessTokenResponseModel token = getAccessToken();
            if (token == null) return Collections.emptyList();

            String url = googleUrlFactory.buildUpcomingMeetingsUrl(userId); // Make sure signature matches
            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());

            ResponseEntity<GoogleScheduledMeetingResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, GoogleScheduledMeetingResponse.class);

            if (response.getBody() == null || response.getBody().getMeetings() == null)
                return Collections.emptyList();

            return response.getBody().getMeetings();

        } catch (HttpClientErrorException.TooManyRequests ex) {
            int wait = Optional.ofNullable(ex.getResponseHeaders().getFirst("Retry-After"))
                    .map(Integer::parseInt)
                    .orElse(10);
            try { Thread.sleep(wait * 1000L);} catch (Exception ignored) {}
            return fetchScheduledMeetings(userId, attempt + 1);

        } catch (Exception ex) {
            log.error("{} - ERROR: {}", method, ex.getMessage());
            return Collections.emptyList();
        }
    }


    // ---------------------------
    // FETCH COMPLETED MEETINGS
    // ---------------------------
    @Override
    @Retryable(
        retryFor = {ResourceAccessException.class, IOException.class},
        maxAttempts = MAX_API_RETRIES,
        backoff = @Backoff(delay = 3000, multiplier = 2)
    )
    public List<GoogleCompletedMeetingDto> fetchCompletedMeetings(String userId) {
        return fetchCompletedMeetings(userId, 1);
    }

    private List<GoogleCompletedMeetingDto> fetchCompletedMeetings(String userId, int attempt) {
        String method = "fetchCompletedMeetings()";
        log.info("{} - Attempt {} for {}", method, attempt, userId);

        if (attempt > MAX_API_RETRIES) return Collections.emptyList();

        try {
            AccessTokenResponseModel token = getAccessToken();
            if (token == null) return Collections.emptyList();

            LocalDate date = LocalDate.now().minusDays(2);
            String url = googleUrlFactory.buildCompletedMeetingsUrl(userId, date); // Implement this method in URL factory
            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());

            ResponseEntity<GoogleCompletedMeetingResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, GoogleCompletedMeetingResponse.class);

            if (response.getBody() == null || response.getBody().getMeetings() == null)
                return Collections.emptyList();

            return response.getBody().getMeetings();

        } catch (HttpClientErrorException.TooManyRequests ex) {
            int wait = Optional.ofNullable(ex.getResponseHeaders().getFirst("Retry-After"))
                    .map(Integer::parseInt)
                    .orElse(10);
            try { Thread.sleep(wait * 1000L);} catch (Exception ignored) {}
            return fetchCompletedMeetings(userId, attempt + 1);

        } catch (Exception ex) {
            log.error("{} - ERROR: {}", method, ex.getMessage());
            return Collections.emptyList();
        }
    }


    // ---------------------------
    // RECOVERY HANDLER
    // ---------------------------
    @Recover
    public List<GoogleScheduledMeetingDto> recoverNetworkFailure(Exception ex, String userId, int attempt) {
        Throwable cause = ex.getCause();

        if (cause instanceof SocketTimeoutException) log.error("RECOVER - Timeout for {}", userId);
        else if (cause instanceof ConnectException) log.error("RECOVER - Connection refused for {}", userId);
        else if (cause instanceof UnknownHostException) log.error("RECOVER - DNS error for {}", userId);
        else if (cause instanceof SSLHandshakeException) log.error("RECOVER - SSL handshake failed for {}", userId);
        else log.error("RECOVER - General failure for {}", userId);

        return Collections.emptyList();
    }


    // ---------------------------
    // HTTP HEADERS
    // ---------------------------
    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        AccessTokenResponseModel token = getAccessToken();
        headers.setBearerAuth(token != null ? token.getAccessToken() : "NULL");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }


    @Override
    public List<GoogleScheduledMeetingDto> fetchRecurringInstances(String masterEventId) {
        String method = "fetchRecurringInstances()";
        log.info("{} - Fetching recurring instances for masterEventId={}", method, masterEventId);

        try {
            AccessTokenResponseModel token = getAccessToken();
            if (token == null) return Collections.emptyList();

            // Build URL for recurring meeting details
            String url = googleUrlFactory.buildRecurringDetailsUrl(masterEventId);
            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());

            ResponseEntity<GoogleScheduledMeetingResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, GoogleScheduledMeetingResponse.class);

            if (response.getBody() == null || response.getBody().getMeetings() == null) {
                log.warn("{} - No recurring instances found for masterEventId={}", method, masterEventId);
                return Collections.emptyList();
            }

            return response.getBody().getMeetings();

        } catch (HttpClientErrorException.TooManyRequests ex) {
            int wait = Optional.ofNullable(ex.getResponseHeaders().getFirst("Retry-After"))
                    .map(Integer::parseInt)
                    .orElse(10);
            try { Thread.sleep(wait * 1000L); } catch (Exception ignored) {}
            // Retry once after wait
            return fetchRecurringInstances(masterEventId);

        } catch (Exception ex) {
            log.error("{} - ERROR fetching recurring instances: {}", method, ex.getMessage());
            return Collections.emptyList();
        }
    }

}
