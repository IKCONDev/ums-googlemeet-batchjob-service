package com.ikn.ums.googlemeet;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "com.ikn.ums.googlemeet")
@Slf4j
@EnableDiscoveryClient
@EnableRetry
public class UmsGooglemeetBatchjobServiceApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		log.info("configure() entered");
		return builder.sources(UmsGooglemeetBatchjobServiceApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(UmsGooglemeetBatchjobServiceApplication.class, args);
	}

	// Default LoadBalanced RestTemplate
	@Bean
	@LoadBalanced
	public RestTemplate createLoadBalancedRestTemplate() {
		log.info("Rest template bean created");
		return new RestTemplate();
	}

	// ModelMapper Bean
	@Bean
	public ModelMapper createMapper() {
		ModelMapper mapper = new ModelMapper();
		mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
		return mapper;
	}

	// Internal Eureka RestTemplate
	@Bean
	@LoadBalanced
	public RestTemplate internalEurekaClientRestTemplate() {
		return new RestTemplate();
	}
	
	@Bean
	public RestTemplate googleRestTemplate() {
	    return new RestTemplate();
	}
	
	@Bean
	public RestTemplate externalIntegrationRestTemplate() {
		return new RestTemplate();
	}

}

 