package com.ikn.ums.googlemeet.externaldto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UMSAttendanceIntervalDto implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@JsonProperty("joinDateTime")
	private String joinDateTime;
	@JsonProperty("leaveDateTime")
	private String leaveDateTime;
	@JsonProperty("durationInSeconds")
	private int attendeeDurationInSeconds;

}
