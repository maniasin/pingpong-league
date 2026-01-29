package com.maniasin.pingpongleague.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiting 서비스
 * - 사용자별 분당 조회 횟수 제한
 * - 메모리 기반 간단한 구현 (실제 운영환경에서는 Redis 기반 구현 권장)
 */
@Service
@Slf4j
public class RateLimitService {

    @Value("${crawler.rate-limit-per-minute:5}")
    private int rateLimitPerMinute;

    // 사용자별 요청 카운터
    private final Map<String, UserRequestCounter> userRequestMap = new ConcurrentHashMap<>();

    /**
     * Rate Limit 체크
     * @param username 사용자명
     * @return true if allowed, false if rate limit exceeded
     */
    public boolean allowRequest(String username) {
        UserRequestCounter counter = userRequestMap.computeIfAbsent(
                username, 
                k -> new UserRequestCounter()
        );

        return counter.tryIncrement(rateLimitPerMinute);
    }

    /**
     * 남은 요청 가능 횟수 반환
     */
    public int getRemainingRequests(String username) {
        UserRequestCounter counter = userRequestMap.get(username);
        if (counter == null) {
            return rateLimitPerMinute;
        }
        return Math.max(0, rateLimitPerMinute - counter.getCount());
    }

    /**
     * 사용자별 요청 카운터 클래스
     */
    private static class UserRequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStartTime = System.currentTimeMillis();
        private static final long WINDOW_SIZE_MS = 60_000; // 1분

        public boolean tryIncrement(int limit) {
            long now = System.currentTimeMillis();
            
            // 시간 윈도우가 지났으면 리셋
            if (now - windowStartTime >= WINDOW_SIZE_MS) {
                synchronized (this) {
                    if (now - windowStartTime >= WINDOW_SIZE_MS) {
                        count.set(0);
                        windowStartTime = now;
                    }
                }
            }

            // 현재 카운트 체크
            int currentCount = count.get();
            if (currentCount >= limit) {
                return false;
            }

            // 증가 시도
            return count.incrementAndGet() <= limit;
        }

        public int getCount() {
            long now = System.currentTimeMillis();
            
            // 시간 윈도우가 지났으면 0 반환
            if (now - windowStartTime >= WINDOW_SIZE_MS) {
                return 0;
            }
            
            return count.get();
        }
    }

    /**
     * 주기적으로 오래된 엔트리 정리 (메모리 누수 방지)
     * 실제 운영환경에서는 @Scheduled 등으로 주기적 정리 필요
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        userRequestMap.entrySet().removeIf(entry -> {
            UserRequestCounter counter = entry.getValue();
            return now - counter.windowStartTime >= UserRequestCounter.WINDOW_SIZE_MS * 2;
        });
    }
}
