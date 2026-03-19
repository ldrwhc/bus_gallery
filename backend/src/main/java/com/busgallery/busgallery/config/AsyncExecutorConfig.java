package com.busgallery.busgallery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncExecutorConfig {

    @Bean(name = "authMailExecutor")
    public Executor authMailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("auth-mail-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(2000);
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }
}

