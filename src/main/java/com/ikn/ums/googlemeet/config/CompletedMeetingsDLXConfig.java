package com.ikn.ums.googlemeet.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CompletedMeetingsDLXConfig {

    @Value("${rabbitmq.dlx.exchange.googlemeet-completed-meetings}")
    private String dlxExchange;

    @Value("${rabbitmq.dlx.queue.googlemeet-completed-meetings}")
    private String dlxQueue;

    @Value("${rabbitmq.dlx.routing-key.googlemeet-completed-meetings}")
    private String dlxRoutingKey;

    @Bean
    public DirectExchange completedDLX() {
        return new DirectExchange(dlxExchange, true, false);
    }

    @Bean
    public Queue completedDLXQueue() {
        return QueueBuilder.durable(dlxQueue).build();
    }

    @Bean
    public Binding completedDLXBinding() {
        return BindingBuilder
                .bind(completedDLXQueue())
                .to(completedDLX())
                .with(dlxRoutingKey);
    }
}
