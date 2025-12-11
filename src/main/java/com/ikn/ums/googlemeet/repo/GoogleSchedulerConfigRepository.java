package com.ikn.ums.googlemeet.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ikn.ums.googlemeet.entity.SchedulerConfig;

public interface GoogleSchedulerConfigRepository extends JpaRepository<SchedulerConfig, Integer> {

    // Find scheduler config by batch type ("Scheduled" or "Past")
    SchedulerConfig findByTypeOfBatch(String typeOfBatch);
}
