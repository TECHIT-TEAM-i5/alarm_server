package com.example.alarm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync// 스프링의 비동기 기능을 활성화하여 Async 어노테이션을 감지
public class AsyncConfig implements AsyncConfigurer {

  @Override
  @Bean(name = "threadPoolTaskExecutor")
  public Executor getAsyncExecutor() {
    int processors = Runtime.getRuntime().availableProcessors(); // 내 PC의 Processor 개수를 가져옴.
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor(); // Thread Pool 관리 클래스
    executor.setCorePoolSize(processors); // 기본 스레드 개수
    executor.setMaxPoolSize(15000); // 최대 스레드 개수 (Queue가 가득찬 이후 maxPoolSize만큼 생성)
    executor.setQueueCapacity(15000); // 대기를 위한 Queue 크기
    executor.setKeepAliveSeconds(60);  // 스레드 재사용 시간
    executor.setThreadNamePrefix("AsyncExecutor-"); // 스레드 이름 prefix
    executor.initialize(); // ThreadPoolExecutor 생성

    return executor;
  }
}
