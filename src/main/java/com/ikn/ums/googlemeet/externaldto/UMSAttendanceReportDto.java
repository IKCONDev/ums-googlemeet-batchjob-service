package com.ikn.ums.googlemeet.externaldto;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UMSAttendanceReportDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// private String odataContext;
	// @JsonProperty("id")
	// private String attendanceReportId;
	@JsonProperty("totalParticipantCount")
	private String totalParticipantCount;
	@JsonProperty("meetingStartDateTime")
	private String meetingStartDateTime;
	@JsonProperty("meetingEndDateTime")
	private String meetingEndDateTime;
	@JsonProperty("attendanceRecords")
	private List<UMSAttendanceRecordDto> attendanceRecords;

}
