package com.ikn.ums.googlemeet.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class TranscriptDto {

    private String name;
    private String state;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private DocsDestination docsDestination;

    @Data
    public static class DocsDestination {
        private String document;
        private String exportUri;
    }
}
