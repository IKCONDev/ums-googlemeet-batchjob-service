package com.ikn.ums.googlemeet.processor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ikn.ums.googlemeet.dto.ConferenceRecordDto;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingAttendeeDto;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingParticipantDto;
import com.ikn.ums.googlemeet.dto.GoogleMeetingDetailsDto;
import com.ikn.ums.googlemeet.dto.GoogleRecurringInstanceDto;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.dto.PlainTranscriptDto;
import com.ikn.ums.googlemeet.dto.TranscriptDto;
import com.ikn.ums.googlemeet.enums.GoogleMeetingType;
import com.ikn.ums.googlemeet.externaldto.EmployeeDto;
import com.ikn.ums.googlemeet.mapper.GoogleMeetingMapper;
import com.ikn.ums.googlemeet.repo.GoogleCompletedMeetingRepository;
import com.ikn.ums.googlemeet.service.GoogleCalendarService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GoogleCompletedMeetingProcessor
        implements MeetingProcessor<GoogleCompletedMeetingDto> {

    // ===================== DEPENDENCIES =====================

    @Autowired
    private GoogleCalendarService googleCalendarService;

    @Autowired
    private GoogleMeetingMapper googlemeetingmapper;

    @Autowired
    private GoogleCompletedMeetingRepository completedMeetingsRepository;

    // ===================== PRE PROCESS =====================

    @Override
    public List<GoogleCompletedMeetingDto> preProcess(List<GoogleCompletedMeetingDto> meetings) {
        log.info("preProcess() called with {} meetings", meetings != null ? meetings.size() : 0);

        if (meetings == null) {
            log.warn("Received null meetings list in preProcess(), returning empty list");
            return Collections.emptyList();
        }

        if (meetings.isEmpty()) {
            log.info("Received empty meetings list, nothing to process");
            return Collections.emptyList();
        }

        log.info("Processing {} meetings", meetings.size());

        return meetings;
    }


    // ===================== DUPLICATE FILTER =====================

    @Override
    public List<GoogleCompletedMeetingDto> filterAlreadyProcessed(List<GoogleCompletedMeetingDto> meetings) {

        final String methodName = "filterAlreadyProcessed()";

        if (meetings == null || meetings.isEmpty()) {
            log.info("{} - No meetings provided. Skipping duplicate check.", methodName);
            return meetings;
        }

        log.info("{} - Starting duplicate filtering for {} incoming meetings, check duplicates by comparing in DB", methodName, meetings.size());

        // Collect all event IDs from incoming meetings
        Set<String> incomingEventIds = meetings.stream()
                .map(GoogleCompletedMeetingDto::getEventid)
                .collect(Collectors.toSet());

        // Fetch existing event IDs from DB
        Set<String> existingEventIds = completedMeetingsRepository.findExistingEventIds(incomingEventIds);

        log.debug("{} - Incoming IDs: {}, Existing IDs in DB: {}",
                methodName, incomingEventIds.size(), existingEventIds.size());

        // Filter out meetings that already exist
        List<GoogleCompletedMeetingDto> filteredResult = meetings.stream()
                .filter(m -> !existingEventIds.contains(m.getEventid()))
                .collect(Collectors.toList());

        log.info(
            "{} - Duplicate filtering completed. Incoming: {}, AlreadyInDB: {}, NewToInsert: {}",
            methodName,
            meetings.size(),
            existingEventIds.size(),
            filteredResult.size()
        );

        return filteredResult;
    }


    // ===================== CLASSIFICATION =====================

    @Override
    public GoogleCompletedMeetingDto classifyType(GoogleCompletedMeetingDto dto) {

        if (dto.getRecurringEventId() != null) {
            dto.setMeetingType(GoogleMeetingType.OCCURRENCE.getValue());
        } else if (dto.getRecurrence() != null && !dto.getRecurrence().isEmpty()) {
            dto.setMeetingType(GoogleMeetingType.RECURRENCE.getValue());
        } else {
            dto.setMeetingType(GoogleMeetingType.SINGLE_INSTANCE.getValue());
        }
        return dto;
    }

    // ===================== INVITEES =====================

    @Override
    public GoogleCompletedMeetingDto attachInvitees(GoogleCompletedMeetingDto meeting) {

        if (meeting == null || meeting.getEventid() == null) {
            log.warn("Meeting has no googleEventId. Skipping invitee fetch.");
            meeting.setAttendees(Collections.emptyList());
            return meeting;
        }

        try {
            List<GoogleCompletedMeetingAttendeeDto> invitees =
                    googleCalendarService.fetchInvitees(
                            meeting.getEventid(),
                            GoogleCompletedMeetingAttendeeDto.class);

            meeting.setAttendees(invitees);

        } catch (Exception ex) {
            log.error("Failed to fetch invitees for Google meeting {} -> {}",
                    meeting.getEventid(), ex.getMessage());
            meeting.setAttendees(Collections.emptyList());
        }

        return meeting;
    }

    // ===================== ENRICH MEETING =====================

    @Override
    public GoogleCompletedMeetingDto enrichMeetingData(GoogleCompletedMeetingDto meeting) {

        final String methodName = "enrichMeetingData()";

        if (meeting == null || meeting.getEventid() == null) {
            log.warn("{} - Meeting object or eventId is null. Skipping enrichment.", methodName);
            return meeting;
        }

        try {
            GoogleMeetingDetailsDto details =
                    googleCalendarService.fetchMeetingDetails(meeting.getEventid());

            if (details == null) return meeting;

            meeting.setHangoutLink(details.getHangoutLink());
            meeting.setTimezone(details.getStart() != null
                    ? details.getStart().getTimeZone()
                    : null);

        } catch (Exception ex) {
            log.error("{} - ERROR enriching eventId={} -> {}",
                    methodName, meeting.getEventid(), ex.getMessage(), ex);
        }

        return meeting;
    }

    // ===================== PARTICIPANTS =====================

    @Override
    public GoogleCompletedMeetingDto attachParticipants(GoogleCompletedMeetingDto meeting) {

        if (meeting == null || meeting.getConferenceRecordId() == null) {
            log.warn("Meeting or conferenceRecordId is null. Skipping participant attachment.");
            return meeting;
        }

        List<GoogleCompletedMeetingParticipantDto> participants =
                googleCalendarService.fetchParticipants(meeting.getConferenceRecordId());

        meeting.setParticipants(
                participants != null ? participants : Collections.emptyList());

        return meeting;
    }

    // ===================== TRANSCRIPTS =====================

    @Override
    public GoogleCompletedMeetingDto attachTranscripts(GoogleCompletedMeetingDto meeting) {

        if (meeting == null || meeting.getConferenceRecordId() == null) {
            log.warn("Meeting or conferenceRecordId is null. Skipping transcript attachment.");
            return meeting;
        }

        List<TranscriptDto> transcripts =
                googleCalendarService.fetchTranscripts(meeting.getConferenceRecordId());

        // Ensure the list is never null
        transcripts = transcripts != null ? transcripts : Collections.emptyList();

        // If there’s at least one transcript, fetch its plain text and set it
        if (!transcripts.isEmpty()) {
            TranscriptDto transcript = transcripts.get(0); // Assuming only one transcript
            if (transcript.getDocsDestination() != null) {
                String plainText = googleCalendarService.fetchPlainTranscriptText(
                        transcript.getDocsDestination().getDocument());
                transcript.setPlainText(plainText); // Assuming you have a 'text' field in TranscriptDto
            }
        }

        // Set transcripts back to meeting
        meeting.setTranscripts(transcripts);

        return meeting;
    }

    // ===================== HELPERS =====================

    private boolean isCompletedMeeting(GoogleCompletedMeetingDto dto) {
        try {
            if (dto.getEndTime() != null) {
                return OffsetDateTime.parse(dto.getEndTime())
                        .isBefore(OffsetDateTime.now(ZoneOffset.UTC));
            }
            if (dto.getStartTime() != null) {
                return OffsetDateTime.parse(dto.getStartTime())
                        .isBefore(OffsetDateTime.now(ZoneOffset.UTC));
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to parse time for meeting {}: {}", dto.getEventid(), e.getMessage());
            return false;
        }
    }
    
 // ------------------ Attach Conference Data ------------------
    
    public GoogleCompletedMeetingDto attachConferenceData(GoogleCompletedMeetingDto meeting) {
 
        if (meeting == null) return null;
 
        // Fetch all conference records
        List<ConferenceRecordDto> records = googleCalendarService.fetchConferenceRecords();
        if (records.isEmpty()) return meeting;
 
        // Find matching conference record
        ConferenceRecordDto matched = records.stream()
                .filter(r -> matchesMeeting(meeting, r))
                .findFirst()
                .orElse(null);
 
        if (matched == null) return meeting;
 
        // Save conferenceRecordId
        meeting.setConferenceRecordId(matched.getName());
 
        // Attach participants
        attachParticipants(meeting);
 
        // Attach transcripts and plain transcripts
        attachTranscripts(meeting);
 
        return meeting;
    }
    private boolean matchesMeeting(GoogleCompletedMeetingDto meeting, ConferenceRecordDto record) {
        OffsetDateTime meetingStart = OffsetDateTime.parse(meeting.getStartTime());
        OffsetDateTime recordStart = record.getStartTime();
        // Allow ±5 minutes tolerance
        return Math.abs(meetingStart.toEpochSecond() - recordStart.toEpochSecond()) <= 300;
    }
    
    
    
    
    
    
    
    
    /**
     * Enriches a completed Zoom meeting with employee/organizer details.
     *
     * <p>This method populates organizational and host-related metadata such as
     * department ID, team ID, and host name based on the provided
     * {@link EmployeeDto}. The enrichment is performed <b>in-memory only</b>
     * and these values are not persisted in the raw Zoom meetings database.</p>
     *
     * <p>This method is invoked as part of the {@code MeetingPipeline} execution
     * flow and is typically called after basic preprocessing and meeting type
     * classification steps.</p>
     *
     * <p>The populated fields are intended solely for downstream communication
     * (e.g., publishing enriched meeting data to the Meeting microservice)
     * and should not be relied upon for persistence-related operations.</p>
     *
     * @param meeting  the completed Zoom meeting DTO to be enriched
     * @param employee the employee context containing department, team,
     *                 and host/organizer details
     * @return the same meeting DTO enriched with employee-specific details
     */
    @Override
    public GoogleCompletedMeetingDto setEmployeeDetails(
    		GoogleCompletedMeetingDto meeting,
            EmployeeDto employee) {

        if (meeting == null || employee == null) {
            return meeting;
        }

        meeting.setDepartmentId(employee.getDepartmentId());
        meeting.setEmailId(employee.getEmail());
        meeting.setTeamId(employee.getTeamId());
       

        return meeting;
    }
}
