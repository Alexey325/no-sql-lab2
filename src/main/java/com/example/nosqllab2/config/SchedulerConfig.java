package com.example.nosqllab2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    // уведомление создаётся за 3 часа до начала показа, в лабораторной №1 это делал setTimeout
    @Bean
    public TaskScheduler notificationTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("notification-scheduler-");
        scheduler.setRemoveOnCancelPolicy(true);

        return scheduler;
    }
}
