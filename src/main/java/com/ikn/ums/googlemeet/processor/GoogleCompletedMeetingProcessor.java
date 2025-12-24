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
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.dto.PlainTranscriptDto;
import com.ikn.ums.googlemeet.dto.TranscriptDto;
import com.ikn.ums.googlemeet.enums.GoogleMeetingType;
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

        List<GoogleCompletedMeetingDto> result = new ArrayList<>();

        log.info("preProcess() -> Starting preprocessing for {} completed meetings", meetings.size());

        for (GoogleCompletedMeetingDto meeting : meetings) {

            log.info("preProcess() -> Processing eventId={}, recurringEventId={}",
                    meeting.getId(), meeting.getRecurringEventId());

            // RECURRING — expand instances
            if (meeting.getRecurringEventId() != null) {

                log.info("preProcess() -> Fetching recurring instances for recurringEventId={}",
                        meeting.getRecurringEventId());

                List<GoogleScheduledMeetingDto> instances =
                        googleCalendarService.fetchRecurringInstances(meeting.getRecurringEventId());

                if (instances == null || instances.isEmpty()) {
                    log.warn("preProcess() -> No instances found for recurringEventId={} -> Skipping",
                            meeting.getRecurringEventId());
                    continue;
                }

                for (GoogleScheduledMeetingDto occ : instances) {

                    GoogleCompletedMeetingDto clone =
                            googlemeetingmapper.cloneCompletedMeeting(meeting);

                    clone.setStart(new GoogleCompletedMeetingDto.EventTime(
                            occ.getStartTime(), occ.getStartTimeZone()));
                    clone.setEnd(new GoogleCompletedMeetingDto.EventTime(
                            occ.getEndTime(), occ.getEndTimeZone()));
                    clone.setRecurringEventId(meeting.getRecurringEventId());

                    result.add(clone);

                    log.info("preProcess() -> Added expanded occurrence -> eventId={}", occ.getId());
                }

            } else {
                // NON-RECURRING
                log.info("preProcess() -> Non-recurring meeting -> Added directly, eventId={}",
                        meeting.getId());
                result.add(meeting);
            }
        }

        log.info("preProcess() -> Preprocessing completed -> Total output meetings={}", result.size());
        return result;
    }

    // ===================== DUPLICATE FILTER =====================

    @Override
    public List<GoogleCompletedMeetingDto> filterAlreadyProcessed(List<GoogleCompletedMeetingDto> meetings) {

        final String methodName = "filterAlreadyProcessed()";

        if (meetings == null || meetings.isEmpty()) {
            log.info("{} - No meetings provided. Skipping duplicate check.", methodName);
            return meetings;
        }

        log.info("{} - Starting duplicate filtering for {} incoming meetings",
                methodName, meetings.size());

        Set<String> incomingEventIds = meetings.stream()
                .map(GoogleCompletedMeetingDto::getId)
                .collect(Collectors.toSet());

        Set<String> existingEventIds =
                completedMeetingsRepository.findExistingEventIds(incomingEventIds);

        log.debug("{} - Incoming IDs: {}, Existing IDs in DB: {}",
                methodName, incomingEventIds.size(), existingEventIds.size());

        List<GoogleCompletedMeetingDto> filteredResult = meetings.stream()
                .filter(m -> !existingEventIds.contains(m.getId()))
                .collect(Collectors.toList());

        log.info("{} - Duplicate filtering completed. Incoming: {}, AlreadyInDB: {}, NewToInsert: {}",
                methodName, meetings.size(), existingEventIds.size(), filteredResult.size());

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

        if (meeting == null || meeting.getId() == null) {
            log.warn("Meeting has no googleEventId. Skipping invitee fetch.");
            meeting.setAttendees(Collections.emptyList());
            return meeting;
        }

        try {
            List<GoogleCompletedMeetingAttendeeDto> invitees =
                    googleCalendarService.fetchInvitees(
                            meeting.getId(),
                            GoogleCompletedMeetingAttendeeDto.class);

            meeting.setAttendees(invitees);

        } catch (Exception ex) {
            log.error("Failed to fetch invitees for Google meeting {} -> {}",
                    meeting.getId(), ex.getMessage());
            meeting.setAttendees(Collections.emptyList());
        }

        return meeting;
    }

    // ===================== ENRICH MEETING =====================

    @Override
    public GoogleCompletedMeetingDto enrichMeetingData(GoogleCompletedMeetingDto meeting) {

        final String methodName = "enrichMeetingData()";

        if (meeting == null || meeting.getId() == null) {
            log.warn("{} - Meeting object or eventId is null. Skipping enrichment.", methodName);
            return meeting;
        }

        try {
            GoogleMeetingDetailsDto details =
                    googleCalendarService.fetchMeetingDetails(meeting.getId());

            if (details == null) return meeting;

            meeting.setHangoutLink(details.getHangoutLink());
            meeting.setTimezone(details.getStart() != null
                    ? details.getStart().getTimeZone()
                    : null);

        } catch (Exception ex) {
            log.error("{} - ERROR enriching eventId={} -> {}",
                    methodName, meeting.getId(), ex.getMessage(), ex);
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

        meeting.setTranscripts(
                transcripts != null ? transcripts : Collections.emptyList());

        List<PlainTranscriptDto> plainTranscripts =
                transcripts != null
                        ? transcripts.stream()
                            .filter(t -> t.getDocsDestination() != null)
                            .map(t -> new PlainTranscriptDto(
                                    t.getName(),
                                    t.getDocsDestination().getDocument(),
                                    googleCalendarService.fetchPlainTranscriptText(
                                            t.getDocsDestination().getDocument())))
                            .toList()
                        : Collections.emptyList();

        meeting.setPlainTranscripts(plainTranscripts);

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
            log.error("Failed to parse time for meeting {}: {}", dto.getId(), e.getMessage());
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
}
