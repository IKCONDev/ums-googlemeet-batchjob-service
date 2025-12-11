package com.ikn.ums.googlemeet.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "scheduler_config")
public class SchedulerConfig {

    @Id
    private Integer cronId;
    private String cronTime;
    private String hour;
    private String minute;
    private String modifiedBy;
    private String typeOfBatch;
}
