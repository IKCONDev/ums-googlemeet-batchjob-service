package com.ikn.ums.googlemeet.externaldto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UMSCompletedMeetingDto {
	
	private Long meetingId;
	private String eventId;
	private String emailId;
	private String originalStartTimeZone;
	private String originalEndTimeZone;
	private String subject;
	private String type;
	private String bodyPreview;
	private String occurrenceId;
	private LocalDateTime startDateTime;
	private LocalDateTime endDateTime;
	private String startTimeZone;
	private String endTimeZone;
	private LocalDateTime ActualStartDateTime;
	private LocalDateTime ActualEndDateTime;
	private String location;
	private Set<UMSCompletedMeetingAttendeeDto> attendees;
	private String organizerEmailId;
	private String organizerName;
	private Long departmentId;
	private Long teamId;
	private String teamName;
	private String onlineMeetingId;
	private String onlineMeetingProvider;
	private String seriesMasterId;
	private String joinUrl;
	private List<UMSTranscriptDto> meetingTranscripts;
	private String insertedBy = "AUTO-BATCH-PROCESS";
	private String insertedDate = LocalDateTime.now().toString();
	private boolean isActionItemsGenerated = false;
	private boolean isManualMeeting = false;
	private Integer batchId;
	private LocalDateTime createdDateTime;
	private LocalDateTime modifiedDateTime;
	private String createdBy;
	private String modifiedBy;
	private String createdByEmailId;
	private String modifiedByEmailId;
	private String actualMeetingDuration;
	private List<UMSAttendanceReportDto> attendanceReport;
	private String departmentName;
	private String attendee;
	private long attendeesCount;

}
