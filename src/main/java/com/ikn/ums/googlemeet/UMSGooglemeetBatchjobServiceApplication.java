package com.ikn.ums.googlemeet;

import java.util.concurrent.Executor;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * @author CheniminiSiriLakshmi
 * @version 1.0
 * 
 */

@EnableCaching
@EnableAspectJAutoProxy(proxyTargetClass = true)
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "com.ikn.ums.googlemeet")
@Slf4j
@EnableDiscoveryClient
@EnableRetry
public class UMSGooglemeetBatchjobServiceApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		log.info("configure() entered");
		return builder.sources(UMSGooglemeetBatchjobServiceApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(UMSGooglemeetBatchjobServiceApplication.class, args);
	}

	// Default LoadBalanced RestTemplate
	@Bean
	@LoadBalanced
	public RestTemplate createLoadBalancedRestTemplate() {
		log.info("Rest template bean created");
		return new RestTemplate();
	}

	@Bean
	public ModelMapper createMapper() {
		ModelMapper mapper = new ModelMapper();
		mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
		return mapper;
	}

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
	
	@Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-exec-");
        executor.initialize();

        log.info("emailTaskExecutor bean initialized");

        return executor;
    }

}

 