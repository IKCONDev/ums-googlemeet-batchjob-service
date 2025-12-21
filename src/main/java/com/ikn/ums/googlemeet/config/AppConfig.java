package com.ikn.ums.googlemeet.config;

import org.springframework.context.annotation.Configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private String timezone;
}

