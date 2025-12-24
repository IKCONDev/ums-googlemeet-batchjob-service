package com.ikn.ums.googlemeet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GoogleCompletedMeetingParticipantDto {

    private String name; 

    @JsonProperty("signedInUser")
    private SignedInUserDto signedinUser; 

    @JsonProperty("earliestStartTime")
    private String earliestStartTime;

    @JsonProperty("latestEndTime")
    private String latestEndTime;

//    @Getter
//    @Setter
//    @AllArgsConstructor
//    @NoArgsConstructor
//    public static class SignedInUser {
//        private String user;         
//        private String displayName;  
//    }
}
