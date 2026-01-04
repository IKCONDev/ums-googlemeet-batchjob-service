package com.ikn.ums.googlemeet.mapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.ikn.ums.googlemeet.dto.EndDto;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingAttendeeDto;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingDto;
import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingParticipantDto;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingAttendeeDto;
import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.dto.StartDto;
import com.ikn.ums.googlemeet.dto.TranscriptDto;
import com.ikn.ums.googlemeet.entity.GoogleCompletedMeeting;
import com.ikn.ums.googlemeet.entity.GoogleScheduledMeeting;
import com.ikn.ums.googlemeet.externaldto.UMSAttendanceIntervalDto;
import com.ikn.ums.googlemeet.externaldto.UMSAttendanceRecordDto;
import com.ikn.ums.googlemeet.externaldto.UMSAttendanceReportDto;
import com.ikn.ums.googlemeet.externaldto.UMSCompletedMeetingAttendeeDto;
import com.ikn.ums.googlemeet.externaldto.UMSCompletedMeetingDto;
import com.ikn.ums.googlemeet.externaldto.UMSScheduledMeetingAttendeeDto;
import com.ikn.ums.googlemeet.externaldto.UMSScheduledMeetingDto;
import com.ikn.ums.googlemeet.externaldto.UMSTranscriptDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GoogleMeetingMapper {

    private final ModelMapper modelMapper;

    public GoogleMeetingMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    /**
     * Maps a GoogleScheduledMeeting JPA entity to its corresponding DTO.
     *
     * @param entity the GoogleScheduledMeeting entity retrieved from the database
     * @return a GoogleScheduledMeetingDto containing mapped meeting details
     */
//    public GoogleScheduledMeetingDto toScheduledGoogleDto(GoogleScheduledMeeting entity) {
//        return modelMapper.map(entity, GoogleScheduledMeetingDto.class);
//    }

    /**
     * Maps a GoogleCompletedMeeting JPA entity to its corresponding DTO.
     *
     * @param entity the GoogleCompletedMeeting entity retrieved from the database
     * @return a GoogleCompletedMeetingDto containing mapped completed meeting details
     */
    public GoogleCompletedMeetingDto toCompletedGoogleDto(GoogleCompletedMeeting entity) {
        return modelMapper.map(entity, GoogleCompletedMeetingDto.class);
    }

    /**
     * Maps a GoogleScheduledMeetingDto to its corresponding JPA entity.
     *
     * @param dto the GoogleScheduledMeetingDto object
     * @return a GoogleScheduledMeeting entity mapped from the DTO
     */
    public GoogleScheduledMeeting toScheduledGoogleEntity(GoogleScheduledMeetingDto dto) {
        return modelMapper.map(dto, GoogleScheduledMeeting.class);
    }

    /**
     * Maps a GoogleCompletedMeetingDto to its corresponding JPA entity.
     *
     * @param dto the GoogleCompletedMeetingDto object
     * @return a GoogleCompletedMeeting entity mapped from the DTO
     */
    public GoogleCompletedMeeting toCompletedGoogleEntity(GoogleCompletedMeetingDto dto) {
        return modelMapper.map(dto, GoogleCompletedMeeting.class);
    }

    /**
     * Maps a list of GoogleScheduledMeeting entities to a list of DTOs.
     *
     * @param entities the list of GoogleScheduledMeeting JPA entities
     * @return a list of GoogleScheduledMeetingDto objects
     */
    public List<GoogleScheduledMeetingDto> toGoogleScheduledDtoList(
            List<GoogleScheduledMeeting> entities) {

        return entities.stream()
                .map(this::toScheduledGoogleDto)
                .collect(Collectors.toList());
    }

    /**
     * Maps a list of GoogleCompletedMeeting entities to a list of DTOs.
     *
     * @param entities the list of GoogleCompletedMeeting JPA entities
     * @return a list of GoogleCompletedMeetingDto objects
     */
    public List<GoogleCompletedMeetingDto> toGoogleCompletedDtoList(
            List<GoogleCompletedMeeting> entities) {

        return entities.stream()
                .map(this::toCompletedGoogleDto)
                .collect(Collectors.toList());
    }

    /**
     * Maps a list of GoogleScheduledMeetingDto objects to a list of JPA entities.
     *
     * @param dtos the list of GoogleScheduledMeetingDto objects
     * @return a list of GoogleScheduledMeeting entities
     */
    public List<GoogleScheduledMeeting> toGoogleScheduledEntityList(
            List<GoogleScheduledMeetingDto> dtos) {

        return dtos.stream()
                .map(this::toScheduledGoogleEntity)
                .collect(Collectors.toList());
    }

    /**
     * Maps a list of GoogleCompletedMeetingDto objects to a list of JPA entities.
     *
     * @param dtos the list of GoogleCompletedMeetingDto objects
     * @return a list of GoogleCompletedMeeting entities
     */
    public List<GoogleCompletedMeeting> toGoogleCompletedEntityList(
            List<GoogleCompletedMeetingDto> dtos) {

        return dtos.stream()
                .map(this::toCompletedGoogleEntity)
                .collect(Collectors.toList());
    }


    /**
     * Converts a GoogleScheduledMeetingDto into a UMSScheduledMeetingDto.
     *
     * <p>This method performs Google Meet–specific transformations, including:</p>
     * <ul>
     *     <li>Mapping Google Calendar event ID as the UMS event and online meeting ID</li>
     *     <li>Converting Google ISO-8601 start and end times into LocalDateTime</li>
     *     <li>Handling recurring meetings using Google recurringEventId</li>
     *     <li>Mapping Google Meet join URL (Hangout link)</li>
     *     <li>Transforming scheduled attendees into UMS attendee structure</li>
     * </ul>
     *
     * <p>The resulting DTO is formatted for UMS scheduled-meeting persistence
     * and downstream batch processing.</p>
     *
     * @param googleDto the GoogleScheduledMeetingDto containing raw Google Meet meeting data
     * @return a UMSScheduledMeetingDto mapped for UMS consumption
     */
    public UMSScheduledMeetingDto toUMSScheduledDto(GoogleScheduledMeetingDto googleDto) {
        UMSScheduledMeetingDto ums = new UMSScheduledMeetingDto();

        ums.setMeetingId(null);
        ums.setEventId(
        	        googleDto.getEventid()
        	);
        ums.setCreatedDateTime(googleDto.getCreated());
        ums.setOriginalStartTimeZone(googleDto.getTimezone());
        ums.setOriginalEndTimeZone(googleDto.getTimezone());
        ums.setSubject(googleDto.getSummary());
        ums.setType(googleDto.getMeetingType());
        ums.setStartDateTime(parseGoogleDateTime(googleDto.getStartTime()));
        ums.setEndDateTime(parseGoogleDateTime(googleDto.getEndTime()));
        ums.setStartTimeZone(googleDto.getTimezone());
        ums.setEndTimeZone(googleDto.getTimezone());
        //ums.setLocation(googleDto.getLocation());
        ums.setLocation("Google Meet Meeting");
        ums.setOrganizerEmailId(googleDto.getHostEmail());
        ums.setOrganizerName(googleDto.getHostName());
        ums.setOnlineMeetingId(googleDto.getEventid());
        ums.setOnlineMeetingProvider("GOOGLE MEET");
        ums.setSeriesMasterId(googleDto.getRecurringEventId());
        ums.setJoinUrl(googleDto.getHangoutLink());
        ums.setOccurrenceId(googleDto.getEventid());
        ums.setInsertedBy("AUTO-BATCH-PROCESS");
        ums.setInsertedDate(LocalDateTime.now().toString());
        ums.setEmailId(googleDto.getEmailId());
        ums.setDepartmentId(googleDto.getDepartmentId());
        ums.setTeamId(googleDto.getTeamId());
        ums.setBatchId(googleDto.getBatchId());
        ums.setDepartmentName(googleDto.getDepartmentName());
        ums.setTeamName(googleDto.getTeamName());

        // Map scheduled attendees
        if (googleDto.getAttendees() != null && !googleDto.getAttendees().isEmpty()) {
            List<UMSScheduledMeetingAttendeeDto> attendees =
                    googleDto.getAttendees().stream()
                             .map(this::mapScheduledAttendee)
                             .collect(Collectors.toList());
            ums.setAttendees(attendees);
        } else {
            ums.setAttendees(Collections.emptyList());
        }

        return ums;
    }
    
    
    /**
     * Converts a list of GoogleScheduledMeetingDto objects into a list of
     * UMSScheduledMeetingDto objects.
     *
     * <p>Each Google scheduled meeting is individually transformed using
     * {@link #toUMSScheduledDto(GoogleScheduledMeetingDto)} to apply
     * Google Meet–specific mapping rules before UMS persistence.</p>
     *
     * @param dtos the list of GoogleScheduledMeetingDto objects
     * @return a list of UMSScheduledMeetingDto objects mapped for UMS consumption
     */
    public List<UMSScheduledMeetingDto> toUMSScheduledDtoList(List<GoogleScheduledMeetingDto> dtos) {
        return dtos.stream().map(this::toUMSScheduledDto).collect(Collectors.toList());
    }

    /**
     * Converts a GoogleCompletedMeetingDto into a UMSCompletedMeetingDto.
     *
     * <p>This method performs Google Meet–specific completed meeting transformations,
     * including:</p>
     * <ul>
     *     <li>Mapping Google Calendar event ID as the UMS event and online meeting ID</li>
     *     <li>Converting Google ISO-8601 start and end times into LocalDateTime</li>
     *     <li>Handling recurring meetings using Google recurringEventId</li>
     *     <li>Mapping invited calendar attendees into UMS completed attendees</li>
     *     <li>Building attendance reports from actual Google Meet participants</li>
     * </ul>
     *
     * <p>The resulting DTO represents a completed meeting with both
     * <b>invited attendees</b> and <b>actual participant presence</b>,
     * formatted for UMS persistence and reporting.</p>
     *
     * @param googleDto the GoogleCompletedMeetingDto containing completed Google Meet meeting data
     * @return a UMSCompletedMeetingDto mapped for UMS persistence
     */
    public UMSCompletedMeetingDto toUMSCompletedDto(GoogleCompletedMeetingDto googleDto) {
        UMSCompletedMeetingDto ums = new UMSCompletedMeetingDto();

        ums.setMeetingId(null);
        ums.setEventId(googleDto.getEventid());
        ums.setEmailId(googleDto.getEmailId());
        ums.setOriginalStartTimeZone(googleDto.getTimezone());
        ums.setOriginalEndTimeZone(googleDto.getTimezone());
        ums.setSubject(googleDto.getSummary());
        ums.setType(googleDto.getMeetingType());
        ums.setStartDateTime(parseGoogleDateTime(googleDto.getStartTime()));
        ums.setEndDateTime(parseGoogleDateTime(googleDto.getEndTime()));
        ums.setStartTimeZone(googleDto.getTimezone());
        ums.setEndTimeZone(googleDto.getTimezone());
        //ums.setLocation(googleDto.getLocation());
        //ums.setOccurrenceId(googleDto.getInstanceEventId());
        ums.setLocation("Google Meet Meeting");
        ums.setOrganizerEmailId(googleDto.getHostEmail());
        ums.setOrganizerName(googleDto.getHostName());
        ums.setOnlineMeetingId(googleDto.getEventid());
        ums.setOnlineMeetingProvider("GOOGLE MEET");
        ums.setSeriesMasterId(googleDto.getRecurringEventId());
        ums.setOccurrenceId(googleDto.getEventid());
        ums.setJoinUrl(googleDto.getHangoutLink());
        ums.setInsertedBy("AUTO-BATCH-PROCESS");
        ums.setInsertedDate(LocalDateTime.now().toString());
        ums.setDepartmentId(googleDto.getDepartmentId());
        ums.setTeamId(googleDto.getTeamId());
        ums.setTeamName(googleDto.getTeamName());
        ums.setDepartmentName(googleDto.getDepartmentName());
        ums.setBatchId(1);
        ums.setCreatedDateTime(LocalDateTime.now());
        ums.setModifiedDateTime(LocalDateTime.now());
        ums.setCreatedBy("AUTO-BATCH-PROCESS");
        ums.setModifiedBy("AUTO-BATCH-PROCESS");
        
        
        // Map attendees (invited emails)
        if (googleDto.getAttendees() != null && !googleDto.getAttendees().isEmpty()) {
            Set<UMSCompletedMeetingAttendeeDto> attendees =
                    googleDto.getAttendees().stream()
                             .map(this::mapCompletedAttendee)
                             .collect(Collectors.toSet());
            ums.setAttendees(attendees);
            ums.setAttendeesCount(attendees.size());
        } else {
            ums.setAttendees(Collections.emptySet());
            ums.setAttendeesCount(0);
        }

        // Map participants (actual presence) into attendance report
        UMSAttendanceReportDto report = buildUMSAttendanceReport(googleDto);
        ums.setAttendanceReport(List.of(report));
        
     // Transcript Mapping
        if (googleDto.getTranscripts() != null) {
//            List<UMSTranscriptDto> transcripts = 
//                googleDto.getTranscripts().stream()
//                             .map(this::mapTranscript)
//                             .collect(Collectors.toList());
        	
        	List<UMSTranscriptDto> umsTranscripts = new ArrayList<>();
        	List<TranscriptDto> googleTranscripts = googleDto.getTranscripts();
        	
        	//map google transcripts to ums transcripts
        	if(googleTranscripts.size() > 0) {
        		googleTranscripts.forEach(trascript -> {
            		UMSTranscriptDto umsTranscript = mapTranscript(trascript, googleDto.getEventid());
            		umsTranscripts.add(umsTranscript);
            	});
        		ums.setMeetingTranscripts(umsTranscripts);
        	}else {
        		ums.setMeetingTranscripts(Collections.emptyList());
        	}
        } else {
            ums.setMeetingTranscripts(Collections.emptyList());
        }


        return ums;
    }
    
    /**
     * Converts a list of GoogleCompletedMeetingDto objects into a list of
     * UMSCompletedMeetingDto objects.
     *
     * <p>Each completed Google Meet meeting is transformed using
     * {@link #toUMSCompletedDto(GoogleCompletedMeetingDto)} to apply
     * Google Meet–specific attendee, participant, and attendance
     * report mapping rules.</p>
     *
     * @param dtos the list of GoogleCompletedMeetingDto objects
     * @return a list of UMSCompletedMeetingDto objects mapped for UMS persistence
     */
    public List<UMSCompletedMeetingDto> toUMSCompletedDtoList(List<GoogleCompletedMeetingDto> dtos) {
        return dtos.stream().map(this::toUMSCompletedDto).collect(Collectors.toList());
    }

   
    private UMSScheduledMeetingAttendeeDto mapScheduledAttendee(GoogleScheduledMeetingAttendeeDto att) {
        UMSScheduledMeetingAttendeeDto dto = new UMSScheduledMeetingAttendeeDto();
        dto.setEmail(att.getEmail());
        dto.setName(att.getEmail() != null ? att.getEmail().split("@")[0] : null);
        dto.setRole(Boolean.TRUE.equals(att.getOrganizer()) ? "Organizer" : "Presenter");
        dto.setType("required");
        dto.setStatus(att.getResponseStatus() != null ? att.getResponseStatus() : "none");
        return dto;
    }

    private UMSCompletedMeetingAttendeeDto mapCompletedAttendee(GoogleCompletedMeetingAttendeeDto att) {
        UMSCompletedMeetingAttendeeDto dto = new UMSCompletedMeetingAttendeeDto();
        dto.setEmail(att.getEmail());
        dto.setEmailId(att.getEmail());
        dto.setType("required");
        dto.setStatus(att.getResponseStatus() != null ? att.getResponseStatus() : "none");
        return dto;
    }

    
    private UMSAttendanceRecordDto mapGoogleParticipantToAttendanceRecord(
            GoogleCompletedMeetingParticipantDto p) {

        UMSAttendanceRecordDto record = new UMSAttendanceRecordDto();

        if (p.getSignedinUser() != null) {
            record.setEmailAddress(p.getSignedinUser().getUser());
        } else if (p.getName() != null) {
            record.setEmailAddress(p.getName()); // fallback to name if userId is missing
        } else {
            record.setEmailAddress("UNKNOWN");
        }

        record.setMeetingRole("Presenter");

        long durationInSeconds = 0;

        try {
            if (p.getEarliestStartTime() != null && !p.getEarliestStartTime().isBlank()
                && p.getLatestEndTime() != null && !p.getLatestEndTime().isBlank()) {

                OffsetDateTime start = OffsetDateTime.parse(p.getEarliestStartTime());
                OffsetDateTime end = OffsetDateTime.parse(p.getLatestEndTime());

                if (end.isAfter(start)) {
                    durationInSeconds = Duration.between(start, end).getSeconds();
                } else {
                    log.warn("Invalid meeting time range -> start={}, end={}",
                             p.getEarliestStartTime(), p.getLatestEndTime());
                }
            }
        } catch (DateTimeParseException ex) {
            log.error("Failed to parse meeting time -> start={}, end={}",
                      p.getEarliestStartTime(), p.getLatestEndTime(), ex);
        }


        // Single interval
        UMSAttendanceIntervalDto interval = new UMSAttendanceIntervalDto();
//        interval.setJoinDateTime(p.getEarliestStartTime() != null ? p.getEarliestStartTime() : "UNKNOWN");
//        interval.setLeaveDateTime(p.getLatestEndTime() != null ? p.getLatestEndTime() : "UNKNOWN");
        interval.setJoinDateTime(toUtcString(p.getEarliestStartTime()));
        interval.setLeaveDateTime(toUtcString(p.getLatestEndTime()));

        //interval.setAttendeeDurationInSeconds(durationInSeconds);

        record.setAttendanceIntervals(List.of(interval));

        return record;
    }


    private String toUtcString(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) return "UNKNOWN";

        return OffsetDateTime.parse(dateTimeStr)
                             .withOffsetSameInstant(ZoneOffset.UTC)
                             .toString();
    }

	private UMSAttendanceReportDto buildUMSAttendanceReport(GoogleCompletedMeetingDto google) {
        UMSAttendanceReportDto report = new UMSAttendanceReportDto();
//        report.setMeetingStartDateTime(
//                google.getStartTime() != null ? google.getStartTime() : "UNKNOWN");
//        report.setMeetingEndDateTime(
//                google.getEndTime() != null ? google.getEndTime() : "UNKNOWN");
        
        report.setMeetingStartDateTime(toUtcString(google.getStartTime()));
        report.setMeetingEndDateTime(toUtcString(google.getEndTime()));


        if (google.getParticipants() != null && !google.getParticipants().isEmpty()) {
            List<UMSAttendanceRecordDto> records = google.getParticipants()
                    .stream()
                    .map(this::mapGoogleParticipantToAttendanceRecord)
                    .collect(Collectors.toList());
            report.setAttendanceRecords(records);
            report.setTotalParticipantCount(String.valueOf(records.size()));
        } else {
            report.setAttendanceRecords(Collections.emptyList());
            report.setTotalParticipantCount("0");
        }

        return report;
    }


   
//    public GoogleScheduledMeetingDto cloneScheduledMeeting(GoogleScheduledMeetingDto source) {
//        return modelMapper.map(source, GoogleScheduledMeetingDto.class);
//    }
    
    public GoogleScheduledMeetingDto cloneScheduledMeeting(GoogleScheduledMeetingDto src) {
        if (src == null) return null;

        GoogleScheduledMeetingDto clone = new GoogleScheduledMeetingDto();

        clone.setEventid(src.getEventid());
        clone.setRecurringEventId(src.getRecurringEventId());
        clone.setSummary(src.getSummary());
        clone.setDescription(src.getDescription());
        clone.setHangoutLink(src.getHangoutLink());
        clone.setLocation(src.getLocation());
        clone.setHostEmail(src.getHostEmail());
        clone.setCreated(src.getCreated());
        clone.setTimezone(src.getTimezone());

        clone.setEmailId(src.getEmailId());
        clone.setDepartmentId(src.getDepartmentId());
        clone.setTeamId(src.getTeamId());
        clone.setBatchId(src.getBatchId());
        clone.setDepartmentName(src.getDepartmentName());
        clone.setTeamName(src.getTeamName());
        clone.setMeetingType(src.getMeetingType());

        // Deep clone recurrence list
        if (src.getRecurrence() != null) {
            clone.setRecurrence(List.copyOf(src.getRecurrence()));
        }

        clone.setInsertedBy(src.getInsertedBy());
        clone.setInsertedDate(src.getInsertedDate());

        // Deep clone attendees
//        if (src.getAttendees() != null) {
//            List<GoogleScheduledMeetingAttendeeDto> attendeeClones = new ArrayList<>();
//            for (GoogleScheduledMeetingAttendeeDto att : src.getAttendees()) {
//                if (att != null) {
//                    GoogleScheduledMeetingAttendeeDto attClone = new GoogleScheduledMeetingAttendeeDto();
//                    attClone.setEmail(att.getEmail());
//                    attClone.setName(att.getName());
//                    attClone.setRole(att.getRole());
//                    attClone.setType(att.getType());
//                    attendeeClones.add(attClone);
//                }
//            }
//            clone.setAttendees(attendeeClones);
//        }

        // Deep clone start
        if (src.getStart() != null) {
            StartDto startClone = new StartDto();
            startClone.setDateTime(src.getStart().getDateTime());
            startClone.setTimeZone(src.getStart().getTimeZone());
            clone.setStart(startClone);
        }

        // Deep clone end
        if (src.getEnd() != null) {
            EndDto endClone = new EndDto();
            endClone.setDateTime(src.getEnd().getDateTime());
            endClone.setTimeZone(src.getEnd().getTimeZone());
            clone.setEnd(endClone);
        }

        clone.setDbid(src.getDbid());

        return clone;
    }


    public GoogleCompletedMeetingDto cloneCompletedMeeting(GoogleCompletedMeetingDto source) {
        return modelMapper.map(source, GoogleCompletedMeetingDto.class);
    }

    
//    private LocalDateTime parseGoogleDateTime(String value) {
//
//        if (value == null || value.isBlank()) {
//            return null;     
//        }
//
//        return OffsetDateTime.parse(value).toLocalDateTime();
//    }
//
    
    
    
    private LocalDateTime parseGoogleDateTime(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return OffsetDateTime.parse(value)
                .withOffsetSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    
    private UMSTranscriptDto mapTranscript(TranscriptDto t, String eventId) {
        UMSTranscriptDto dto = new UMSTranscriptDto();
        if (t == null) return dto;

        // Use transcript name as ID
        dto.setTranscriptId(t.getDocsDestination().getDocument());

        // Transcript content URL
        if (t.getDocsDestination() != null) {
            dto.setTranscriptContentUrl(t.getDocsDestination().getExportUri());

        } else {
            dto.setTranscriptContentUrl(null);
            dto.setTranscriptFilePath(null);
        }

        // Created timestamp
        dto.setCreatedDateTime(t.getStartTime() != null ? t.getStartTime().toString() : null);

        // Transcript content placeholder (fetch separately if needed)
        dto.setTranscriptContent(t.getPlainText());
        dto.setMeetingId(eventId);

        return dto;
    }
    
    
    
    public GoogleScheduledMeetingDto toScheduledGoogleDto(GoogleScheduledMeeting entity) {

        GoogleScheduledMeetingDto dto =
                modelMapper.map(entity, GoogleScheduledMeetingDto.class);

        //MANUAL start mapping
        if (entity.getStartTime() != null) {
            StartDto start = new StartDto();
            start.setDateTime(entity.getStartTime());
            start.setTimeZone(entity.getTimezone());
            dto.setStart(start);
        }

        //MANUAL end mapping
        if (entity.getEndTime() != null) {
            EndDto end = new EndDto();
            end.setDateTime(entity.getEndTime());
            end.setTimeZone(entity.getTimezone());
            dto.setEnd(end);
        }

        //IMPORTANT: also set timezone explicitly
        dto.setTimezone(entity.getTimezone());

        return dto;
    }




}
