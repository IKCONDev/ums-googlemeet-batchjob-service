package com.ikn.ums.googlemeet.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScheduledMeetingsDLXConfig {

    @Value("${rabbitmq.dlx.exchange.scheduled-meetings}")
    private String dlxExchange;

    @Value("${rabbitmq.dlx.queue.scheduled-meetings}")
    private String dlxQueue;

    @Value("${rabbitmq.dlx.routing-key.scheduled-meetings}")
    private String dlxRoutingKey;

    @Bean
    public DirectExchange scheduledDLX() {
        return new DirectExchange(dlxExchange, true, false);
    }

    @Bean
    public Queue scheduledDLXQueue() {
        return QueueBuilder.durable(dlxQueue).build();
    }

    @Bean
    public Binding scheduledDLXBinding() {
        return BindingBuilder
                .bind(scheduledDLXQueue())
                .to(scheduledDLX())
                .with(dlxRoutingKey);
    }
}
