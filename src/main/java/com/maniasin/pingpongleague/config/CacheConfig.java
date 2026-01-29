package com.maniasin.pingpongleague.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 캐시 설정 클래스
 * - Caffeine을 사용한 인메모리 캐싱
 * - 크롤링 결과를 30분간 캐싱하여 중복 크롤링 방지
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PLAYER_RECORDS_CACHE = "playerRecords";
    public static final String CRAWLING_JOBS_CACHE = "crawlingJobs";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                PLAYER_RECORDS_CACHE,
                CRAWLING_JOBS_CACHE
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats());
        
        return cacheManager;
    }
}
