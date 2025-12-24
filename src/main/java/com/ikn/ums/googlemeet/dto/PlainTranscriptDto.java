package com.ikn.ums.googlemeet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlainTranscriptDto {

    private String transcriptName;   // conferenceRecords/.../transcripts/{uuid}
    private String documentId;       // Docs fileId
    private String plainText;  // Drive export text/plain
}
