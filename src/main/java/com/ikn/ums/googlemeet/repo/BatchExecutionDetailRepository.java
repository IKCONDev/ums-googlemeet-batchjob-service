package com.ikn.ums.googlemeet.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ikn.ums.googlemeet.entity.BatchExecutionDetail;


public interface BatchExecutionDetailRepository
        extends JpaRepository<BatchExecutionDetail, Long> {
}
