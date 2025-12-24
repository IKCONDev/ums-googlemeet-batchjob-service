package com.ikn.ums.googlemeet.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ikn.ums.googlemeet.constants.RabbitMQConstants;

@Configuration
public class ScheduledMeetingsQueueConfig {

    @Value("${rabbitmq.exchange.googlemeet-scheduled-meetings}")
    private String exchangeName;

    @Value("${rabbitmq.queue.googlemeet-scheduled-meetings}")
    private String queueName;

    @Value("${rabbitmq.routing-key.googlemeet-scheduled-meetings}")
    private String routingKey;

    @Value("${rabbitmq.dlx.exchange.googlemeet-scheduled-meetings}")
    private String dlxExchange;

    @Value("${rabbitmq.dlx.routing-key.googlemeet-scheduled-meetings}")
    private String dlxRoutingKey;

    @Bean
    public DirectExchange scheduledMeetingsExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue scheduledMeetingsQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument(RabbitMQConstants.DLX_EXCHANGE_KEY, dlxExchange)
                .withArgument(RabbitMQConstants.DLX_ROUTING_KEY, dlxRoutingKey)
                .build();
    }

    @Bean
    public Binding scheduledMeetingsBinding() {
        return BindingBuilder
                .bind(scheduledMeetingsQueue())
                .to(scheduledMeetingsExchange())
                .with(routingKey);
    }
}

