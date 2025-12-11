package com.ikn.ums.googlemeet.externaldto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentDto {

	private Long departmentId;
	private String departmentName;
	private String departmentAddress;
	private String departmentCode;
}
