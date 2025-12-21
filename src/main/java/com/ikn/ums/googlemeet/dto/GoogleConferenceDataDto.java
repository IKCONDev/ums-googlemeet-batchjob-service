//package com.ikn.ums.googlemeet.dto;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import lombok.Data;
//
//import java.util.List;
//
//@Data
//public class GoogleConferenceDataDto {
//
//    @JsonProperty("entryPoints")
//    private List<GoogleEntryPointDto> entryPoints;  // dial-in, meet link
//
//    @JsonProperty("conferenceId")
//    private String conferenceId;
//}



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
