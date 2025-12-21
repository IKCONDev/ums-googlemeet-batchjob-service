package com.ikn.ums.googlemeet.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ConferenceRecordDto {

	 @JsonProperty("name")
	 private String name;

	 @JsonProperty("startTime")
	 private OffsetDateTime startTime;

	 @JsonProperty("endTime")
	 private OffsetDateTime endTime;

	 @JsonProperty("expireTime")
	 private OffsetDateTime expireTime;

	 @JsonProperty("space")
	 private String space;
	 
	 private List<GoogleCompletedMeetingParticipantDto> participants = new ArrayList<>();

	 private List<TranscriptDto> transcripts = new ArrayList<>();
}
