package com.ikn.ums.googlemeet.processor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;

@Service
public class GoogleScheduledMeetingProcessor
        implements MeetingProcessor<GoogleScheduledMeetingDto> {

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
        return meeting;
    }

    /**
     * Preprocess scheduled meetings
     * - Filters out past meetings if desired, but here we include all upcoming meetings
     * - Classifies the type
     */
    @Override
    public List<GoogleScheduledMeetingDto> preProcess(
            List<GoogleScheduledMeetingDto> meetings) {

        return meetings.stream()
                .filter(this::isScheduledMeeting)  // Keep only valid scheduled meetings
                .map(this::classifyType)
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
}
