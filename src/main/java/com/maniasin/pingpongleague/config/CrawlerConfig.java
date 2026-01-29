package com.maniasin.pingpongleague.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

/**
 * 크롤링 관련 설정 클래스
 * - 동시 실행 제한 (Semaphore)
 * - ThreadPool 설정
 */
@Configuration
@EnableAsync
@Getter
public class CrawlerConfig {

    @Value("${crawler.max-concurrent-jobs:3}")
    private int maxConcurrentJobs;

    @Value("${crawler.cache-duration-minutes:30}")
    private int cacheDurationMinutes;

    @Value("${crawler.rate-limit-per-minute:5}")
    private int rateLimitPerMinute;

    /**
     * 동시 실행 가능한 크롤링 작업 수를 제한하는 Semaphore
     * 최대 3개의 크롤링 작업만 동시에 실행
     */
    @Bean
    public Semaphore crawlerSemaphore() {
        return new Semaphore(maxConcurrentJobs);
    }

    /**
     * 크롤링 작업용 Job Executor
     * - 각 크롤링 작업(Job) 전체를 실행하는 스레드 풀
     */
    @Bean(name = "crawlerJobExecutor")
    public Executor crawlerJobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(maxConcurrentJobs);
        executor.setMaxPoolSize(maxConcurrentJobs * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("crawler-job-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 크롤링 사이트별 Task Executor
     * - 한 작업 내에서 여러 사이트를 병렬로 크롤링하는 스레드 풀
     */
    @Bean(name = "crawlerTaskExecutor")
    public Executor crawlerTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("crawler-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
