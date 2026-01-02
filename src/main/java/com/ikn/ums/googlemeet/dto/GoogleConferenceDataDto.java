package com.ikn.ums.googlemeet.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleConferenceDataDto {
    private String conferenceId;
    private GoogleConferenceSolutionDto conferenceSolution;
    private List<GoogleEntryPointDto> entryPoints;
}
