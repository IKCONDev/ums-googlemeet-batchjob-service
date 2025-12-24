package com.ikn.ums.googlemeet.processor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingAttendeeDto;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.service.GoogleCalendarService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GoogleScheduledMeetingProcessor
        implements MeetingProcessor<GoogleScheduledMeetingDto> {
	
	 @Autowired
	    private GoogleCalendarService googleCalendarService;

    /**
     * Classifies a scheduled meeting type.
     * Recurrence rules and recurringEventId determine the type.
     */
    @Override
    public GoogleScheduledMeetingDto classifyType(GoogleScheduledMeetingDto dto) {

        if (dto.getRecurringEventId() != null) {
            dto.setMeetingType("OCCURRENCE");  // Could also use enum if needed
            return dto;
        }

        if (dto.getRecurrence() != null && !dto.getRecurrence().isEmpty()) {
            dto.setMeetingType("RECURRENCE");
            return dto;
        }

        dto.setMeetingType("SINGLE_INSTANCE");
        return dto;
    }

    /**
     * Attach invitees if needed (placeholder for future logic)
     */
    @Override
    public GoogleScheduledMeetingDto attachInvitees(GoogleScheduledMeetingDto meeting) {

        try {

            if (meeting.getId() == null) {
                log.warn("Scheduled meeting has no googleEventId. Skipping invitee fetch.");
                meeting.setAttendees(Collections.emptyList());
                return meeting;
            }

            // Fetch invitees from Google Calendar Event
            List<GoogleScheduledMeetingAttendeeDto> invitees =
                    googleCalendarService.fetchInvitees(
                            meeting.getId(),
                            GoogleScheduledMeetingAttendeeDto.class
                    );

            meeting.setAttendees(invitees);

            log.debug("Fetched {} invitees for scheduled Google meeting {}",
                      invitees.size(), meeting.getId());

        } catch (Exception ex) {

            log.error("Failed to fetch invitees for scheduled Google meeting {} -> {}",
                      meeting.getId(), ex.getMessage());

            meeting.setAttendees(Collections.emptyList());
        }

        return meeting;
    }


    /**
     * Preprocess scheduled meetings
     * - Filters out past meetings if desired, but here we include all upcoming meetings
     * - Classifies the type
     */
    /**
     * Preprocess scheduled Google meetings
     * - Filters only valid scheduled meetings
     * - Classifies meeting type (SINGLE_INSTANCE, RECURRING, etc.)
     * - Removes duplicates based on eventId
     */
    @Override
    public List<GoogleScheduledMeetingDto> preProcess(List<GoogleScheduledMeetingDto> meetings) {

        if (meetings == null || meetings.isEmpty()) {
            return Collections.emptyList();
        }

        // Filter valid meetings, classify type, and deduplicate by eventId
        return meetings.stream()
                .filter(this::isScheduledMeeting)      // Keep only valid meetings
                .map(this::classifyType)               // Set meetingType
                .collect(Collectors.toMap(
                        GoogleScheduledMeetingDto::getId, // key = eventId
                        m -> m,                           // value = dto
                        (existing, duplicate) -> existing // keep first occurrence
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
    }


    /**
     * Determines if a meeting is valid as a scheduled meeting
     * For scheduled meetings, we include only future or ongoing meetings
     */
    private boolean isScheduledMeeting(GoogleScheduledMeetingDto dto) {
        try {
            return dto.getEndTime() != null &&
                   OffsetDateTime.parse(dto.getEndTime())
                       .isAfter(OffsetDateTime.now(ZoneOffset.UTC));
        } catch (Exception e) {
            return false;
        }
    }

	@Override
	public GoogleScheduledMeetingDto attachConferenceData(GoogleScheduledMeetingDto meeting) {
		// TODO Auto-generated method stub
		return null;
	}
}
