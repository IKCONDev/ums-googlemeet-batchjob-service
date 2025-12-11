package com.ikn.ums.googlemeet.cache;

import java.util.List;

import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Component;

import com.ikn.ums.googlemeet.externaldto.EmployeeDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CacheWriter {

    @CachePut(value = CacheNames.GET_ALL_ACTIVE_EMPLOYEES_CACHE, key = "'getAllActiveEmployees'")
    public List<EmployeeDto> cacheActiveEmployeesList(List<EmployeeDto> employees) {
        log.info("cacheActiveEmployeesList() Active employees cached: {}", employees.size());
        return employees;
    }
}
