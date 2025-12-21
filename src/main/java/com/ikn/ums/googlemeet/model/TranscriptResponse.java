package com.ikn.ums.googlemeet.model;

import java.util.List;
import com.ikn.ums.googlemeet.dto.TranscriptDto;
import lombok.Data;

@Data
public class TranscriptResponse {
    private List<TranscriptDto> transcripts;
}
