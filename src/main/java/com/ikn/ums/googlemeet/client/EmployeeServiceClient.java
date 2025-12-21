package com.ikn.ums.googlemeet.client;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


import com.ikn.ums.googlemeet.cache.CacheWriter;
import com.ikn.ums.googlemeet.constants.CacheNames;
import com.ikn.ums.googlemeet.externaldto.EmployeeDto;
import com.ikn.ums.googlemeet.externaldto.EmployeeListWrapperDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EmployeeServiceClient {

    @Autowired
    @Qualifier("internalEurekaClientRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheWriter cacheWriter;

    private static final String GET_ALL_ACTIVE_EMPLOYEES_URL =
            "http://UMS-EMPLOYEE-SERVICE/employees/get-all";

    private static final String GET_ALL_ACTIVE_EMPLOYEES_CACHE_KEY =
            CacheNames.GET_ALL_ACTIVE_EMPLOYEES_CACHE + "::getAllActiveEmployees";

    @Retry(name = "employeeServiceRetry", fallbackMethod = "getAllActiveEmployeesFallBack")
    @CircuitBreaker(name = "employeeServiceCircuit", fallbackMethod = "getAllActiveEmployeesFallBack")
    public List<EmployeeDto> getEmployeesListFromEmployeeService() {

        String method = "getEmployeesListFromEmployeeService()";
        log.info("{} - Calling employee microservice...", method);

        ResponseEntity<EmployeeListWrapperDto> response =
                restTemplate.exchange(
                        GET_ALL_ACTIVE_EMPLOYEES_URL,
                        HttpMethod.GET,
                        null,
                        EmployeeListWrapperDto.class
                );

        EmployeeListWrapperDto wrapper = response.getBody();

        if (wrapper == null || wrapper.getEmployee() == null || wrapper.getEmployee().isEmpty()) {
            log.warn("{} - Employee service returned null or empty", method);
            throw new IllegalStateException("Employee service returned null/empty response");
        }

        List<EmployeeDto> employees = wrapper.getEmployee();
        log.info("{} - Received {} employees", method, employees.size());

        cacheWriter.cacheActiveEmployeesList(employees);

        return employees;
    }

    public List<EmployeeDto> getAllActiveEmployeesFallBack(Throwable ex) {

        String methodName = "getAllActiveEmployeesFallBack()";
        log.error("{} - Fallback hit! Employee service DOWN. Reason: {}", methodName, ex.toString());

        List<EmployeeDto> cacheData = getActiveEmployeesFromCache();

        if (!cacheData.isEmpty()) {
            log.warn("{} - Returning {} employees from Redis fallback cache", methodName, cacheData.size());
            return cacheData;
        }

        log.warn("{} - Redis cache empty. Returning empty list from fallback.", methodName);
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<EmployeeDto> getActiveEmployeesFromCache() {

        log.info("Fetching employees from Redis key: {}", GET_ALL_ACTIVE_EMPLOYEES_CACHE_KEY);

        Object cachedObj = redisTemplate.opsForValue().get(GET_ALL_ACTIVE_EMPLOYEES_CACHE_KEY);

        if (cachedObj == null) {
            log.warn("No data found in Redis for key: {}", GET_ALL_ACTIVE_EMPLOYEES_CACHE_KEY);
            return Collections.emptyList();
        }

        if (cachedObj instanceof List<?>) {
            List<EmployeeDto> employees = (List<EmployeeDto>) cachedObj;
            log.info("Fetched {} employees from Redis cache", employees.size());
            return employees;
        }

        log.error("Unexpected data type in Redis for key {}: {}",
                GET_ALL_ACTIVE_EMPLOYEES_CACHE_KEY, cachedObj.getClass().getName());

        return Collections.emptyList();
    }
}
