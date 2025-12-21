package com.ikn.ums.googlemeet.model;

import java.util.List;

import com.ikn.ums.googlemeet.dto.GoogleCompletedMeetingParticipantDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class GoogleCompletedMeetingParticipantsResponse {

    private List<GoogleCompletedMeetingParticipantDto> participants;
}
