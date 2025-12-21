package com.ikn.ums.googlemeet.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.model.AccessTokenResponseModel;
import com.ikn.ums.googlemeet.service.GoogleCompletedMeetingService;
import com.ikn.ums.googlemeet.service.GoogleScheduledMeetingService;
import com.ikn.ums.googlemeet.utils.InitializeGoogleOAuth;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * @author CheniminiSiriLakshmi
 * Central REST controller for managing Google Meet integration operations.
 * 
 * <p>
 * Provides endpoints that support communication with the Google Meet platform,
 * including token generation, event data processing, and other Meet
 * functionalities as the application evolves. Processing responsibilities
 * are delegated to service-layer components to maintain modularity.
 * </p>
 * 
 */

@RestController
@RequestMapping("/googlemeet")

@Slf4j
public class GoogleMeetSourceDataBatchProcessController {
	
	@Autowired
	private InitializeGoogleOAuth googleOAuth;
	
	@Autowired
	private GoogleCompletedMeetingService meetingservice;

	@Autowired
    private GoogleScheduledMeetingService googleScheduledMeetingService;
	
	@GetMapping("/auth/token")
	public ResponseEntity<AccessTokenResponseModel> authenticateGoogleMeet() {
	    
	    String methodName = "authenticateGoogleMeet()";
	    log.info("{} - STARTED", methodName);

	    AccessTokenResponseModel token =  googleOAuth.getAccessToken();

	    log.info("{} - COMPLETED - Token fetched successfully", methodName);

	    return new ResponseEntity<>(token, HttpStatus.OK);
	}
	
	
	/**
     * Trigger batch fetch of completed Google Meet events for all configured users.
     */
	@Transactional
    @GetMapping("/completed-meetings/batch")
    public ResponseEntity<List<GoogleCompletedMeetingDto>> performCompletedMeetingsRawDataBatchProcessing() {
        String methodName = "performCompletedMeetingsRawDataBatchProcessing()";
        log.info("{} - STARTED - Triggering batch fetch of completed Google Meet events", methodName);

        try {
            List<GoogleCompletedMeetingDto> completedMeetingsList = 
                    meetingservice.performMeetingsRawDataBatchProcessing();

            log.info("{} - COMPLETED - Total events fetched: {}", 
                     methodName, completedMeetingsList.size());

            return new ResponseEntity<>(completedMeetingsList, HttpStatus.OK);

        } catch (Exception ex) {
            log.error("{} - EXCEPTION occurred while processing completed meetings: {}",
                    methodName, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Collections.emptyList());
        }
    }
    
    
    
    @GetMapping("/scheduledMeetingsBatchProcess")
    public ResponseEntity<List<GoogleScheduledMeetingDto>> performScheduledMeetingsRawDataBatchProcessing() {

        String methodName = "performScheduledMeetingsRawDataBatchProcessing()";
        log.info("{} - STARTED - Triggering batch fetch of scheduled Google meetings", methodName);

        try {

            List<GoogleScheduledMeetingDto> scheduledMeetingsList =
                    googleScheduledMeetingService.performScheduledMeetingsRawDataBatchProcessing();

            log.info("{} - COMPLETED - Total meetings fetched: {}", 
                     methodName, scheduledMeetingsList.size());

            return new ResponseEntity<>(scheduledMeetingsList, HttpStatus.OK);

        } catch (Exception ex) {

            log.error("{} - EXCEPTION occurred while processing Google scheduled meetings: {}",
                    methodName, ex.getMessage(), ex);

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());

            // throw new ControllerException(
            //     ErrorCodeMessages.ERR_GOOGLE_SCHEDULED_BATCH_EXCEPTION_CODE,
            //     ErrorCodeMessages.ERR_GOOGLE_SCHEDULED_BATCH_EXCEPTION_MSG);
        }
    }

}

