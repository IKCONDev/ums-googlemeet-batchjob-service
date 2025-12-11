package com.ikn.ums.googlemeet.utils;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.ikn.ums.googlemeet.model.AccessTokenResponseModel;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Component
public class InitializeGoogleOAuth {

    @Value("${google.clientId}")
    private String clientId;

    @Value("${google.clientSecret}")
    private String clientSecret;

    @Value("${google.refreshToken}")
    private String refreshToken;

    @Value("${google.token-url}")
    private String tokenUrl;  // e.g., https://oauth2.googleapis.com/token

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Fetch Google access token via HTTP POST (with retries on network failures)
     */
    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public AccessTokenResponseModel getAccessToken() {
        String methodName = "getAccessToken()";
        log.info("{} - STARTED generating Google access token", methodName);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Build body for refresh token grant
            String body = "client_id=" + clientId +
                          "&client_secret=" + clientSecret +
                          "&refresh_token=" + refreshToken +
                          "&grant_type=refresh_token";

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            log.info("{} - Sending token request to Google...", methodName);
            ResponseEntity<AccessTokenResponseModel> response =
                    restTemplate.exchange(tokenUrl, HttpMethod.POST, entity, AccessTokenResponseModel.class);

            AccessTokenResponseModel token = response.getBody();

            if (token == null || token.getAccessToken() == null) {
                log.error("{} - Google returned NULL token", methodName);
                return null;
            }

            log.info("{} - Token generated successfully. Expires in {} seconds", methodName, token.getExpiresIn());
            return token;

        } catch (HttpClientErrorException ex) {
            log.error("{} - HttpClientErrorException: {}", methodName, ex.getMessage());
            return null;

        } catch (Exception ex) {
            log.error("{} - Exception occurred: {}", methodName, ex.getMessage(), ex);
            throw ex;
        }
    }

    @Recover
    public AccessTokenResponseModel recoverToken(Exception ex) {
        log.error("RECOVER - Google token generation FAILED after all retries. Cause: {}", ex.getMessage(), ex);
        return null;
    }
    
    
    
    
    public String getBaseUrl() {
        return "https://www.googleapis.com/calendar/v3";
    }

    public String getAccessTokenString() {
        AccessTokenResponseModel token = getAccessToken();
        return token != null ? token.getAccessToken() : null;
    }

}
