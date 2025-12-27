package com.ikn.ums.googlemeet.externaldto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeDto implements Serializable{
	
	    private static final long serialVersionUID = 1L;
	
		private Integer id;

		private String googleUserId;

		private String firstName;

		private String lastName;

		private String email;

		private String designation;

		private Long departmentId;
		
		private Long teamId;
		
		private DepartmentDto department;
		
		private String employeeStatus;
		
		private String batchProcessStatus;

}
