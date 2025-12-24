package com.ikn.ums.googlemeet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class DocsDestinationDto {
	
	private String document;
    private String exportUri;

}
