package com.ikn.ums.googlemeet.externaldto;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class DesignationDto implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Long id;
	
	private String designationName;
	
	private String active;
	
	private LocalDateTime createdDateTime;
	
	private LocalDateTime modifiedDateTime;
	
	private String createdBy;
	
	private String modifiedBy;
	
	private String createdByEmailId;
	
	private String modifiedByEmailId;
	

}
