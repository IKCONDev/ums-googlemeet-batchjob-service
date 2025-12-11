package com.ikn.ums.googlemeet.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.ikn.ums.googlemeet.dto.GoogleScheduledMeetingDto;
import com.ikn.ums.googlemeet.entity.GoogleScheduledMeeting;
import com.ikn.ums.googlemeet.externaldto.UMSScheduledMeetingDto;
import com.ikn.ums.googlemeet.externaldto.UMSCompletedMeetingDto;

@Component
public class GoogleMeetingMapper {

    private final ModelMapper modelMapper;

    public GoogleMeetingMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    // ----------------- Entity ↔ DTO -----------------

    public GoogleScheduledMeetingDto toScheduledGoogleDto(GoogleScheduledMeeting entity) {
        return modelMapper.map(entity, GoogleScheduledMeetingDto.class);
    }

    public GoogleScheduledMeeting toScheduledGoogleEntity(GoogleScheduledMeetingDto dto) {
        return modelMapper.map(dto, GoogleScheduledMeeting.class);
    }

    public List<GoogleScheduledMeetingDto> toGoogleScheduledDtoList(List<GoogleScheduledMeeting> entities) {
        return entities.stream()
                       .map(this::toScheduledGoogleDto)
                       .collect(Collectors.toList());
    }

    public List<GoogleScheduledMeeting> toGoogleScheduledEntityList(List<GoogleScheduledMeetingDto> dtos) {
        return dtos.stream()
                   .map(this::toScheduledGoogleEntity)
                   .collect(Collectors.toList());
    }

    // ----------------- DTO → UMS DTO -----------------

    public UMSScheduledMeetingDto toUMSScheduledDto(GoogleScheduledMeetingDto googleDto) {
        UMSScheduledMeetingDto ums = new UMSScheduledMeetingDto();

        ums.setMeetingId(googleDto.getId());
        ums.setEventId(googleDto.getGoogleEventId());
        ums.setCreatedDateTime(googleDto.getCreatedAt());
        ums.setOriginalStartTimeZone(googleDto.getTimezone());
        ums.setOriginalEndTimeZone(googleDto.getTimezone());
        ums.setSubject(googleDto.getSummary());
        ums.setType(googleDto.getMeetingType());
        ums.setStartDateTime(parseGoogleDateTime(googleDto.getStartTime()));
        ums.setEndDateTime(parseGoogleDateTime(googleDto.getEndTime()));
        ums.setStartTimeZone(googleDto.getTimezone());
        ums.setEndTimeZone(googleDto.getTimezone());
        ums.setLocation(googleDto.getLocation());
        ums.setOrganizerEmailId(googleDto.getOrganizerEmail());
        ums.setOnlineMeetingId(googleDto.getGoogleEventId());
        ums.setOnlineMeetingProvider("GOOGLE_MEET");
        ums.setSeriesMasterId(googleDto.getRecurringEventId());
        ums.setJoinUrl(googleDto.getJoinUrl());
        ums.setInsertedBy("AUTO-BATCH-PROCESS");
        ums.setInsertedDate(LocalDateTime.now().toString());

        ums.setEmailId(googleDto.getEmailId());
        ums.setDepartmentId(googleDto.getDepartmentId());
        ums.setTeamId(googleDto.getTeamId());
        ums.setBatchId(googleDto.getBatchId());
        ums.setDepartmentName(googleDto.getDepartmentName());
        ums.setTeamName(googleDto.getTeamName());

        return ums;
    }

    public List<UMSScheduledMeetingDto> toUMSDtoScheduledList(List<GoogleScheduledMeetingDto> dtos) {
        return dtos.stream()
                   .map(this::toUMSScheduledDto)
                   .collect(Collectors.toList());
    }

    public List<UMSScheduledMeetingDto> toUMSScheduledDtoListFromEntity(List<GoogleScheduledMeeting> entities) {
        return entities.stream()
                       .map(this::toScheduledGoogleDto)
                       .map(this::toUMSScheduledDto)
                       .collect(Collectors.toList());
    }

    // ----------------- Completed Meetings -----------------

    public UMSCompletedMeetingDto toUMSCompletedDto(GoogleScheduledMeetingDto googleDto) {
        UMSCompletedMeetingDto ums = new UMSCompletedMeetingDto();

        ums.setMeetingId(null); // PK handled by DB
        ums.setEventId(googleDto.getGoogleEventId());
        ums.setCreatedDateTime(googleDto.getCreatedAt());
        ums.setOriginalStartTimeZone(googleDto.getTimezone());
        ums.setOriginalEndTimeZone(googleDto.getTimezone());
        ums.setSubject(googleDto.getSummary());
        ums.setType(googleDto.getMeetingType());
        ums.setStartDateTime(parseGoogleDateTime(googleDto.getStartTime()));
        ums.setEndDateTime(parseGoogleDateTime(googleDto.getEndTime()));
        ums.setStartTimeZone(googleDto.getTimezone());
        ums.setEndTimeZone(googleDto.getTimezone());
        ums.setLocation(googleDto.getLocation());
        ums.setOrganizerEmailId(googleDto.getOrganizerEmail());
        ums.setOnlineMeetingId(googleDto.getGoogleEventId());
        ums.setOnlineMeetingProvider("GOOGLE_MEET");
        ums.setSeriesMasterId(googleDto.getRecurringEventId());
        ums.setJoinUrl(googleDto.getJoinUrl());
        ums.setInsertedBy("AUTO-BATCH-PROCESS");
        ums.setInsertedDate(LocalDateTime.now().toString());

        ums.setEmailId(googleDto.getEmailId());
        ums.setDepartmentId(googleDto.getDepartmentId());
        ums.setTeamId(googleDto.getTeamId());
        ums.setBatchId(googleDto.getBatchId());
        ums.setDepartmentName(googleDto.getDepartmentName());
        ums.setTeamName(googleDto.getTeamName());

        return ums;
    }

    public List<UMSCompletedMeetingDto> toUMSCompletedDtoList(List<GoogleScheduledMeetingDto> dtos) {
        return dtos.stream()
                   .map(this::toUMSCompletedDto)
                   .collect(Collectors.toList());
    }

    // ----------------- Utility -----------------

    public GoogleScheduledMeetingDto cloneScheduledMeeting(GoogleScheduledMeetingDto source) {
        return modelMapper.map(source, GoogleScheduledMeetingDto.class);
    }

    private LocalDateTime parseGoogleDateTime(String googleDateTime) {
        return OffsetDateTime.parse(googleDateTime).toLocalDateTime();
    }
}
