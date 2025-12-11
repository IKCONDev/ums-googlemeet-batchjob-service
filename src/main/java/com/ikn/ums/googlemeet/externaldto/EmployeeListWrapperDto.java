package com.ikn.ums.googlemeet.externaldto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeListWrapperDto {
	
   private List<EmployeeDto> employee;

}
