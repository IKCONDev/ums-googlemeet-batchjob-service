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
public class UMSAttendanceRecordDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String emailAddress;
	@JsonProperty("totalAttendanceInSeconds")
	private int totalAttendanceInSeconds;
	@JsonProperty("role")
	private String meetingRole;
	@JsonProperty("attendanceIntervals")
	private List<UMSAttendanceIntervalDto> attendanceIntervals;

}
