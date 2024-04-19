package com.example.alarm.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlarmConfig {
    @Bean
    public Queue queue(){
        return new Queue(
                "boot.amqp.alarm-queue",
                true,
                false,
                true
        );
    }

    @Bean
    public Queue authQueue(){
        return new Queue(
                "boot.amqp.auth-queue",
                true,
                false,
                true
        );
    }


}
