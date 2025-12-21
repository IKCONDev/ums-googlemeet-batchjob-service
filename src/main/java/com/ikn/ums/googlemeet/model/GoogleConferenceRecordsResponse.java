package com.ikn.ums.googlemeet.model;

import java.util.List;

import com.ikn.ums.googlemeet.dto.ConferenceRecordDto;

import lombok.Data;

@Data
public class GoogleConferenceRecordsResponse {

    private List<ConferenceRecordDto> conferenceRecords;

//    @Data
//    public static class ConferenceRecord {
//        private String name;       
//        private String startTime;
//        private String endTime;
//        private String expireTime;
//        private String space;
//    }
}
