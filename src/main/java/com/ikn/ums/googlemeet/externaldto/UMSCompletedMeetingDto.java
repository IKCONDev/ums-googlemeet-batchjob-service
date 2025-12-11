package com.ikn.ums.googlemeet.externaldto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UMSCompletedMeetingDto implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Long meetingId;
	private String eventId;
	private String createdDateTime;
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
	private String location;
	private String organizerEmailId;
	private String organizerName;
	private String onlineMeetingId;
	private String onlineMeetingProvider;
	private String seriesMasterId;
	private String joinUrl;
	private String insertedBy = "AUTO-BATCH-PROCESS";
	private String insertedDate = LocalDateTime.now().toString();
	private String emailId;
	private Long departmentId;
	private Long teamId;
	private Long batchId;
	private String departmentName;
	private String TeamName;
	private List<UMSScheduledMeetingAttendeeDto> attendees;

}
