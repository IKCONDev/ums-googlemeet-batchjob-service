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

import com.ikn.ums.googlemeet.dto.EndDto;
import com.ikn.ums.googlemeet.dto.GoogleRecurringInstanceDto;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingAttendeeDto;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.dto.StartDto;
import com.ikn.ums.googlemeet.enums.GoogleMeetingType;
import com.ikn.ums.googlemeet.exception.EmptyInputException;
import com.ikn.ums.googlemeet.externaldto.EmployeeDto;
import com.ikn.ums.googlemeet.mapper.GoogleMeetingMapper;
import com.ikn.ums.googlemeet.repo.GoogleScheduledMeetingRepository;
import com.ikn.ums.googlemeet.service.GoogleCalendarService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GoogleScheduledMeetingProcessor
        implements MeetingProcessor<GoogleScheduledMeetingDto> {

    private final EmptyInputException emptyInputException;
	
	 @Autowired
	    private GoogleCalendarService googleCalendarService;
	 
	 @Autowired
	 private GoogleMeetingMapper googlemeetingmapper;
	 
	 @Autowired
	 private GoogleScheduledMeetingRepository scheduledMeetingsRepository;

    GoogleScheduledMeetingProcessor(EmptyInputException emptyInputException) {
        this.emptyInputException = emptyInputException;
    }

    /**
     * Classifies a scheduled meeting type.
     * Recurrence rules and recurringEventId determine the type.
     */
    @Override
    public GoogleScheduledMeetingDto classifyType(GoogleScheduledMeetingDto dto) {

        if (dto.getRecurringEventId() != null) {
            dto.setMeetingType(GoogleMeetingType.RECURRENCE.getValue()); 
            return dto;
        }

        if (dto.getRecurrence() != null && !dto.getRecurrence().isEmpty()) {
            dto.setMeetingType(GoogleMeetingType.RECURRENCE.getValue());
            return dto;
        }

        dto.setMeetingType(GoogleMeetingType.SINGLE_INSTANCE.getValue());
        return dto;
    }

    /**
     * Attach invitees if needed (placeholder for future logic)
     */
    @Override
    public GoogleScheduledMeetingDto attachInvitees(GoogleScheduledMeetingDto meeting) {

        try {

            if (meeting.getEventid() == null) {
                log.warn("Scheduled meeting has no googleEventId. Skipping invitee fetch.");
                meeting.setAttendees(Collections.emptyList());
                return meeting;
            }

            // Fetch invitees from Google Calendar Event
            List<GoogleScheduledMeetingAttendeeDto> invitees =
                    googleCalendarService.fetchInvitees(
                            meeting.getEventid(),
                            GoogleScheduledMeetingAttendeeDto.class
                    );

            meeting.setAttendees(invitees);

            log.debug("Fetched {} invitees for scheduled Google meeting {}",
                      invitees.size(), meeting.getEventid());

        } catch (Exception ex) {

            log.error("Failed to fetch invitees for scheduled Google meeting {} -> {}",
                      meeting.getEventid(), ex.getMessage());

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

        List<GoogleScheduledMeetingDto> result = new ArrayList<>();

        log.info("preProcess() -> Starting preprocessing for {} scheduled meetings", meetings.size());

        for (GoogleScheduledMeetingDto meeting : meetings) {

            log.info("preProcess() -> Processing eventId={}, recurringEventId={}",
                    meeting.getEventid(), meeting.getRecurringEventId());

            // Check if this is a recurring meeting
            if (meeting.getRecurringEventId() != null) {

                log.info("preProcess() -> Recurring meeting detected -> recurringEventId={}", meeting.getRecurringEventId());

                // Fetch recurring instances using Google Calendar API
                List<GoogleRecurringInstanceDto> instances =
                        googleCalendarService.fetchRecurringInstances(meeting.getRecurringEventId());

                if (instances == null || instances.isEmpty()) {
                    // No instances found, optionally keep only the parent
                    log.warn("preProcess() -> No instances found for recurringEventId={} -> Keeping only parent", meeting.getRecurringEventId());
                    GoogleScheduledMeetingDto cloneParent = googlemeetingmapper.cloneScheduledMeeting(meeting);
                    result.add(cloneParent);
                    continue;
                }

                // Expand instances
                for (GoogleRecurringInstanceDto occ : instances) {

                    // Deep clone the parent meeting
                    GoogleScheduledMeetingDto clone = googlemeetingmapper.cloneScheduledMeeting(meeting);

                    // Map instance start/end times
                    clone.setStart(new StartDto(
                            occ.getStart().getDateTime(),
                            occ.getStart().getTimeZone()
                    ));
                    clone.setEnd(new EndDto(
                            occ.getEnd().getDateTime(),
                            occ.getEnd().getTimeZone()
                    ));

                    // Maintain recurringEventId reference
                    clone.setRecurringEventId(meeting.getRecurringEventId());

                    result.add(clone);

                    log.info("preProcess() -> Added expanded occurrence -> instanceEventId={}", occ.getInstanceEventId());
                }

            } else {
                // Non-recurring meeting
                log.info("preProcess() -> Non-recurring meeting -> Added directly, eventId={}", meeting.getEventid());
                GoogleScheduledMeetingDto clone = googlemeetingmapper.cloneScheduledMeeting(meeting);
                result.add(clone);
            }
        }

        log.info("preProcess() -> Preprocessing completed -> Total output meetings={}", result.size());
        return result;
    }


    
    @Override
    public List<GoogleScheduledMeetingDto> filterAlreadyProcessed(List<GoogleScheduledMeetingDto> meetings) {

        final String methodName = "filterAlreadyProcessed()";

        if (meetings == null || meetings.isEmpty()) {
            log.info("{} - No meetings provided. Skipping duplicate check.", methodName);
            return meetings;
        }

        log.info("{} - Starting duplicate filtering for {} incoming meetings",
                methodName, meetings.size());

        Set<String> incomingEventIds = meetings.stream()
                .map(GoogleScheduledMeetingDto::getEventid)
                .collect(Collectors.toSet());

        Set<String> existingEventIds =
        		scheduledMeetingsRepository.findExistingEventIds(incomingEventIds);

        log.debug("{} - Incoming IDs: {}, Existing IDs in DB: {}",
                methodName, incomingEventIds.size(), existingEventIds.size());

        List<GoogleScheduledMeetingDto> filteredResult = meetings.stream()
                .filter(m -> !existingEventIds.contains(m.getEventid()))
                .collect(Collectors.toList());

        log.info("{} - Duplicate filtering completed. Incoming: {}, AlreadyInDB: {}, NewToInsert: {}",
                methodName, meetings.size(), existingEventIds.size(), filteredResult.size());

        return filteredResult;
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
    public GoogleScheduledMeetingDto setEmployeeDetails(
    		GoogleScheduledMeetingDto meeting,
            EmployeeDto employee) {

        if (meeting == null || employee == null) {
            return meeting;
        }

        meeting.setDepartmentId(employee.getDepartmentId());
        meeting.setEmailId(employee.getEmail());
        meeting.setTeamId(employee.getTeamId());
        meeting.setHostName(employee.getFirstName()+" "+employee.getLastName());
        meeting.setHostEmail(employee.getEmail());
       
        return meeting;
    }
}
