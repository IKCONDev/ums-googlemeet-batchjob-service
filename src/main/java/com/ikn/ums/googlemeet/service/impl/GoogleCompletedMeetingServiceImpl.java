package com.ikn.ums.googlemeet.service.impl;
 
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
 
import com.ikn.ums.googlemeet.client.EmployeeServiceClient;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.entity.GoogleCompletedMeeting;
import com.ikn.ums.googlemeet.externaldto.EmployeeDto;
import com.ikn.ums.googlemeet.externaldto.UMSCompletedMeetingDto;
import com.ikn.ums.googlemeet.mapper.GoogleMeetingMapper;
import com.ikn.ums.googlemeet.service.GoogleAsyncService;
import com.ikn.ums.googlemeet.service.GoogleCompletedMeetingService;
import com.ikn.ums.googlemeet.service.GoogleMeetingPersistenceService;
import com.ikn.ums.googlemeet.service.GoogleMeetingsQueuePublisherService;
import com.ikn.ums.zoom.utils.AbstractBatchExecutor; // Assuming batch executor is generic
 
import lombok.extern.slf4j.Slf4j;
 
/**
* Service implementation for processing Google Meet meetings.
* Handles API calls, async execution, retries, DTO/entity mapping,
* and persistence of meeting data.
*/
@Slf4j
@Service
public class GoogleCompletedMeetingServiceImpl extends AbstractBatchExecutor implements GoogleCompletedMeetingService {
 
    @Autowired
    private EmployeeServiceClient employeeServiceClient;
 
    @Autowired
    private GoogleMeetingMapper googleMeetingMapper;
    
    @Autowired
    private GoogleMeetingPersistenceService googleMeetingPersistenceService;
    
    @Autowired
    private GoogleMeetingsQueuePublisherService meetingsQueuePublisherService;
    
    @Autowired
    private GoogleAsyncService googleAsyncService;
    
    private static final int BATCH_SIZE = 20;
    
    @Override
    public List<GoogleCompletedMeetingDto> performMeetingsRawDataBatchProcessing() {
 
        final String methodName = "performMeetingsRawDataBatchProcessing()";
        log.info("{} - STARTED", methodName);
 
        List<EmployeeDto> employeeList =
                employeeServiceClient.getEmployeesListFromEmployeeService();
 
        log.info("{} - Fetched employee list - count={}",
                methodName, employeeList != null ? employeeList.size() : 0);
 
        if (employeeList == null || employeeList.isEmpty()) {
            log.warn("{} - No employees found. Skipping processing.", methodName);
            return Collections.emptyList();
        }
 
        List<String> emailIds = employeeList.stream()
                .map(EmployeeDto::getEmail)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
 
        log.info("{} - Prepared emailIds list - count={}", methodName, emailIds.size());
 
        List<List<String>> emailBatches =
                partition(emailIds, BATCH_SIZE);
        
        log.info("{} - Partitioned {} users into {} batch(es) with batchSize={}",
                methodName,
                emailIds.size(),
                emailBatches.size(),
                BATCH_SIZE);
        
        List<GoogleCompletedMeetingDto> masterList =
        		executeInBatches(emailBatches,
        				googleAsyncService::getCompletedMeetingsAsync,
        				"CompletedMeetingsBatch");
 
        log.info("{} - All batches completed. Total meetings fetched={}",
                methodName, masterList.size());
 
        List<GoogleCompletedMeeting> meetingEntities =
                googleMeetingMapper.toGoogleCompletedEntityList(masterList);
        
//     // 1️⃣ Map DTOs to entities
//        List<GoogleCompletedMeeting> meetingEntities =
//                googleMeetingMapper.toGoogleCompletedEntityList(masterList);

        // 2️⃣ REMOVE DUPLICATES based on meeting ID or conferenceRecordId
        Map<String, GoogleCompletedMeeting> uniqueMeetings = new LinkedHashMap<>();
        for (GoogleCompletedMeeting m : meetingEntities) {
            if (m.getId() != null) {
                uniqueMeetings.putIfAbsent(m.getId(), m);
            } else if (m.getConferenceRecordId() != null) {
                uniqueMeetings.putIfAbsent(m.getConferenceRecordId(), m);
            }
        }
        List<GoogleCompletedMeeting> dedupedMeetings = new ArrayList<>(uniqueMeetings.values());

        // 3️⃣ Link child entities
        linkMeetingToChildEntities(dedupedMeetings);

        // 4️⃣ Persist deduplicated meetings
        List<GoogleCompletedMeeting> savedMeetings =
                googleMeetingPersistenceService.persistCompletedMeetings(dedupedMeetings);

 
        log.info("{} - Mapped DTO to entity list - count={}",
                methodName, meetingEntities.size());
 
        linkMeetingToChildEntities(meetingEntities);
        log.info("{} - Linked meeting objects to child entities", methodName);
 
//        List<GoogleCompletedMeeting> savedMeetings =
//                googleMeetingPersistenceService.persistCompletedMeetings(meetingEntities);
 
        log.info("{} - Persisted completed meetings - savedCount={}",
                methodName, savedMeetings.size());
 
        List<GoogleCompletedMeetingDto> finalDtoList =
                googleMeetingMapper.toGoogleCompletedDtoList(savedMeetings);
 
        log.info("{} - Mapped saved entities to DTO list - count={}",
                methodName, finalDtoList.size());
 
        List<UMSCompletedMeetingDto> umsDtoList =
                googleMeetingMapper.toUMSCompletedDtoList(finalDtoList);
 
        meetingsQueuePublisherService
                .publishCompletedMeetingsBatchEventInQueue(umsDtoList);
 
        log.info("{} - CompletedMeetingsBatchEvent published - recordCount={}",
                methodName, umsDtoList.size());
 
        log.info("{} - COMPLETED. Total meetings saved={}",
                methodName, finalDtoList.size());
 
        return finalDtoList;
    }
 
    
    /**
     * Links each meeting entity to its child entities (attendees, participants, transcripts)
     * for proper JPA persistence.
     */
    private void linkMeetingToChildEntities(List<GoogleCompletedMeeting> meetings) {
 
        final String methodName = "linkMeetingToChildEntities()";
 
        if (meetings == null || meetings.isEmpty()) {
            log.info("{} - No meeting entities to link.", methodName);
            return;
        }
 
        log.info("{} - Linking child entities for {} meetings", methodName, meetings.size());
 
        for (GoogleCompletedMeeting meeting : meetings) {
 
            if (meeting.getAttendees() != null && !meeting.getAttendees().isEmpty()) {
                meeting.getAttendees().forEach(a -> a.setMeeting(meeting));
            }
 
            if (meeting.getParticipants() != null && !meeting.getParticipants().isEmpty()) {
                meeting.getParticipants().forEach(p -> p.setMeeting(meeting));
            }
            
            if (meeting.getTranscripts() != null && !meeting.getTranscripts().isEmpty()) {
            	meeting.getTranscripts().forEach(t -> t.setMeeting(meeting));
            }
        }
 
        log.info("{} - Linking completed.", methodName);
    }
    
    
}
 
 