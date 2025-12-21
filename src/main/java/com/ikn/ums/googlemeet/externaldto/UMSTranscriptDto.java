package com.ikn.ums.googlemeet.externaldto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UMSTranscriptDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer id;
	private String transcriptId;
	private String meetingId;
	private String meetingOrganizerId;
	private String transcriptContentUrl;
	private String createdDateTime;
	private String transcriptFilePath;
	private String transcriptContent;

}
