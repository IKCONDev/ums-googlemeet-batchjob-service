package com.ikn.ums.googlemeet.processor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ikn.ums.googlemeet.dto.ConferenceRecordDto;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingAttendeeDto;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingParticipantDto;
import com.ikn.ums.googlemeet.dto.TranscriptDto;
import com.ikn.ums.googlemeet.enums.GoogleMeetingType;
import com.ikn.ums.googlemeet.service.GoogleCalendarService;

import lombok.extern.slf4j.Slf4j;

/**
 * Processor for Google Completed Meetings.
 *
 * <p>This processor handles classification, attendee attachment, participants
 * attachment, and transcript enrichment for completed Google Meet events.</p>
 */
@Service
@Slf4j
public class GoogleCompletedMeetingProcessor
        implements MeetingProcessor<GoogleCompletedMeetingDto> {

    @Autowired
    private GoogleCalendarService googleCalendarService;

    // ------------------ Classification ------------------

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

    // ------------------ Attach Invitees ------------------

    @Override
    public GoogleCompletedMeetingDto attachInvitees(GoogleCompletedMeetingDto meeting) {

        try {

            if (meeting.getGoogleEventId() == null) {
                log.warn("Meeting has no googleEventId. Skipping invitee fetch.");
                meeting.setAttendees(Collections.emptyList());
                return meeting;
            }

            // Fetch attendees from Google Calendar Event
            List<GoogleCompletedMeetingAttendeeDto> invitees =
                    googleCalendarService.fetchInvitees(
                            meeting.getGoogleEventId(),
                            GoogleCompletedMeetingAttendeeDto.class
                    );

            meeting.setAttendees(invitees);

            log.debug("Fetched {} invitees for Google meeting {}",
                      invitees.size(), meeting.getGoogleEventId());

        } catch (Exception ex) {

            log.error("Failed to fetch invitees for Google meeting {} -> {}",
                      meeting.getGoogleEventId(), ex.getMessage());

            meeting.setAttendees(Collections.emptyList());
        }

        return meeting;
    }


    // ------------------ Pre-Process Meetings ------------------

    @Override
    public List<GoogleCompletedMeetingDto> preProcess(List<GoogleCompletedMeetingDto> meetings) {
        return meetings.stream()
                .filter(this::isCompletedMeeting)
                .map(this::classifyType)
                .collect(Collectors.toList());
    }

    private boolean isCompletedMeeting(GoogleCompletedMeetingDto dto) {
        try {
            return dto.getEndTime() != null &&
                    OffsetDateTime.parse(dto.getEndTime())
                            .isBefore(OffsetDateTime.now(ZoneOffset.UTC));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Fetches and attaches participant details for this completed Google Meet meeting.
     *
     * <p>Participant information is available only for completed meetings through
     * the Google Meet Conference Records API
     * (e.g., /conferenceRecords/{conferenceRecordId}/participants).</p>
     *
     * @param meeting the completed Google meeting DTO to enrich
     * @return the same meeting DTO with attached participant details when available
     */
    @Override
    public GoogleCompletedMeetingDto attachParticipants(GoogleCompletedMeetingDto meeting) {

        final String methodName = "attachParticipants()";

        if (meeting == null || meeting.getConferenceRecordId() == null) {
            log.warn("{} - Meeting or conferenceRecordId is null. Skipping participant attachment.",
                    methodName);
            return meeting;
        }

        log.info("{} - Fetching participants for conferenceRecordId={}",
                methodName, meeting.getConferenceRecordId());

        List<GoogleCompletedMeetingParticipantDto> participants =
                googleCalendarService.fetchParticipants(
                        meeting.getConferenceRecordId()
                );

        log.info("{} - Retrieved {} participants for conferenceRecordId={}",
                methodName,
                participants != null ? participants.size() : 0,
                meeting.getConferenceRecordId());

        meeting.setParticipants(
                participants != null ? participants : Collections.emptyList()
        );

        log.debug("{} - Participant attachment completed for conferenceRecordId={}",
                methodName, meeting.getConferenceRecordId());

        return meeting;
    }
    
    
    
    /**
     * Fetches and attaches transcripts for this completed Google Meet meeting.
     *
     * <p>Completed Google Meet meetings may have transcripts generated
     * if transcription was enabled during the meeting.</p>
     *
     * @param meeting the completed Google meeting DTO to enrich
     * @return the same meeting DTO with transcript data attached when available
     */
    @Override
    public GoogleCompletedMeetingDto attachTranscripts(GoogleCompletedMeetingDto meeting) {

        final String methodName = "attachTranscripts()";

        if (meeting == null || meeting.getConferenceRecordId() == null) {
            log.warn("{} - Meeting or conferenceRecordId is null. Skipping transcript attachment.",
                    methodName);
            return meeting;
        }

        String conferenceRecordId = meeting.getConferenceRecordId();
        log.info("{} - Fetching transcripts for conferenceRecordId={}",
                methodName, conferenceRecordId);

        List<TranscriptDto> transcripts =
                googleCalendarService.fetchTranscripts(conferenceRecordId);

        if (transcripts != null && !transcripts.isEmpty()) {
            log.info("{} - Retrieved {} transcripts for conferenceRecordId={}",
                    methodName, transcripts.size(), conferenceRecordId);
            meeting.setTranscripts(transcripts);
        } else {
            log.warn("{} - No transcripts available for conferenceRecordId={}",
                    methodName, conferenceRecordId);
            meeting.setTranscripts(Collections.emptyList());
        }

        log.debug("{} - Transcript attachment completed for conferenceRecordId={}",
                methodName, conferenceRecordId);

        return meeting;
    }

    
    public GoogleCompletedMeetingDto attachConferenceData(GoogleCompletedMeetingDto meeting) {

        // 1. Fetch conference records
        List<ConferenceRecordDto> records =
                googleCalendarService.fetchConferenceRecords();

        if (records.isEmpty()) {
            return meeting;
        }

        // 2. Match conference record with meeting time
        ConferenceRecordDto matched =
                records.stream()
                       .filter(r -> matchesMeeting(meeting, r))
                       .findFirst()
                       .orElse(null);

        if (matched == null) {
            return meeting;
        }

        // 3. Save conferenceRecordId
        meeting.setConferenceRecordId(matched.getName());

        // 4. Fetch participants
        meeting.setParticipants(
                googleCalendarService.fetchParticipants(matched.getName())
        );

        // 5. Fetch transcripts
        meeting.setTranscripts(
                googleCalendarService.fetchTranscripts(matched.getName())
        );

        return meeting;
    }

    private boolean matchesMeeting(GoogleCompletedMeetingDto meeting,
            ConferenceRecordDto record) {

OffsetDateTime meetingStart =
OffsetDateTime.parse(meeting.getStartTime());

OffsetDateTime recordStart = record.getStartTime();

// allow ±5 minutes tolerance
return Math.abs(
meetingStart.toEpochSecond() -
recordStart.toEpochSecond()
) <= 300;
}




}
