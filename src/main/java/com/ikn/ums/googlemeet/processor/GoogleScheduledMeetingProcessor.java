package com.ikn.ums.googlemeet.processor;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.enums.GoogleMeetingType;
import com.ikn.ums.googlemeet.mapper.GoogleMeetingMapper;
import com.ikn.ums.googlemeet.service.GoogleCalendarService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GoogleScheduledMeetingProcessor implements MeetingProcessor<GoogleScheduledMeetingDto> {

    @Autowired
    private GoogleCalendarService googleapiService;

    @Autowired
    private GoogleMeetingMapper googleMeetingMapper;

    @Override
    public GoogleScheduledMeetingDto classifyType(GoogleScheduledMeetingDto meeting) {
        try {
            GoogleMeetingType meetingType =
                    (meeting.getRecurringEventId() != null && !meeting.getRecurringEventId().isEmpty())
                            ? GoogleMeetingType.RECURRENCE
                            : GoogleMeetingType.SINGLE_INSTANCE;

            meeting.setMeetingType(meetingType.getValue());
            log.debug("Meeting {} classified as {}", meeting.getGoogleEventId(), meetingType);

        } catch (Exception ex) {
            log.error("Failed to classify meeting {} → {}", meeting.getGoogleEventId(), ex.getMessage());
        }
        return meeting;
    }

    @Override
    public GoogleScheduledMeetingDto attachInvitees(GoogleScheduledMeetingDto meeting) {
        
        return meeting;
    }

    @Override
    public List<GoogleScheduledMeetingDto> preProcess(List<GoogleScheduledMeetingDto> meetings) {
        List<GoogleScheduledMeetingDto> result = new ArrayList<>();

        for (GoogleScheduledMeetingDto meeting : meetings) {
            // Recurring events
            if (meeting.getRecurringEventId() != null && !meeting.getRecurringEventId().isEmpty()) {
                List<GoogleScheduledMeetingDto> childEvents = googleapiService.fetchRecurringInstances(meeting.getGoogleEventId());
                if (childEvents != null && !childEvents.isEmpty()) {
                    for (GoogleScheduledMeetingDto child : childEvents) {
                        GoogleScheduledMeetingDto clone = googleMeetingMapper.cloneScheduledMeeting(meeting);
                        clone.setStartTime(child.getStartTime());
                        clone.setEndTime(child.getEndTime());
                        clone.setGoogleEventId(child.getGoogleEventId());
                        clone.setMeetingType("OCCURRENCE");
                        result.add(clone);
                    }
                }
            } else {
                // Single instance
                result.add(meeting);
            }
        }
        return result;
    }

    @Override
    public List<GoogleScheduledMeetingDto> filterDateRange(
            List<GoogleScheduledMeetingDto> meetings,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime) {

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        return meetings.stream()
                .filter(m -> {
                    if (m.getStartTime() == null) return false;

                    try {
                        LocalDateTime meetingDateTime =
                                ZonedDateTime.parse(m.getStartTime(), formatter)
                                             .toLocalDateTime();
                        return !meetingDateTime.isBefore(startDateTime)
                                && !meetingDateTime.isAfter(endDateTime);
                    } catch (Exception ex) {
                        log.warn("Failed to parse meeting startTime: {} → {}", m.getStartTime(), ex.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }
}
