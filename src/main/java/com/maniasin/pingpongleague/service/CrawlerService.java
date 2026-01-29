package com.maniasin.pingpongleague.service;

import com.maniasin.pingpongleague.config.CacheConfig;
import com.maniasin.pingpongleague.domain.CrawlingCache;
import com.maniasin.pingpongleague.domain.Player;
import com.maniasin.pingpongleague.dto.JobProgress;
import com.maniasin.pingpongleague.repository.CrawlingCacheRepository;
import com.maniasin.pingpongleague.repository.PlayerRepository;
import com.maniasin.pingpongleague.service.crawler.SiteCrawler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CrawlerService {

    private final List<SiteCrawler> crawlers;
    private final PlayerRepository playerRepository;
    private final CrawlingCacheRepository crawlingCacheRepository;
    @Qualifier("crawlerJobExecutor")
    private final Executor crawlerJobExecutor;
    @Qualifier("crawlerTaskExecutor")
    private final Executor crawlerTaskExecutor;
    private final Semaphore crawlerSemaphore; // 동시 실행 제한용
    private final Map<String, JobProgress> jobStatus = new ConcurrentHashMap<>();
    
    @Value("${crawler.cache-duration-minutes:30}")
    private int cacheDurationMinutes;
    
    // 중복 크롤링 방지: 현재 진행 중인 작업 추적
    private final Map<String, CompletableFuture<String>> ongoingCrawls = new ConcurrentHashMap<>();

    /**
     * 크롤링 작업 시작
     * - 동일 선수에 대한 중복 크롤링 방지
     * - 이미 진행 중인 작업이 있으면 해당 JobId 반환
     */
    public String startCrawlingJob(String playerName) {
        log.info("크롤링 작업 요청: '{}'", playerName);
        
        // 1. 이미 진행 중인 작업이 있는지 확인
        CompletableFuture<String> ongoingJob = ongoingCrawls.get(playerName);
        if (ongoingJob != null && !ongoingJob.isDone()) {
            try {
                String existingJobId = ongoingJob.get(100, TimeUnit.MILLISECONDS);
                log.info("동일 선수 크롤링이 이미 진행 중입니다. 기존 JobId 반환: {}", existingJobId);
                return existingJobId;
            } catch (TimeoutException e) {
                // Job이 아직 생성 중이므로 조금 더 기다림
                try {
                    String existingJobId = ongoingJob.get(5, TimeUnit.SECONDS);
                    log.info("동일 선수 크롤링이 이미 진행 중입니다. 기존 JobId 반환: {}", existingJobId);
                    return existingJobId;
                } catch (Exception ex) {
                    log.warn("기존 작업 확인 실패, 새 작업 시작: {}", ex.getMessage());
                }
            } catch (Exception e) {
                log.warn("기존 작업 확인 실패, 새 작업 시작: {}", e.getMessage());
            }
        }

        // 2. 새로운 작업 생성
        String jobId = UUID.randomUUID().toString();
        JobProgress progress = JobProgress.builder()
                .status("QUEUED")
                .percentage(0)
                .message("크롤링 대기 중... (현재 진행 중인 작업이 많아 잠시 대기합니다)")
                .build();
        jobStatus.put(jobId, progress);

        // 3. 작업 시작 CompletableFuture 생성 및 등록
        CompletableFuture<String> jobFuture = CompletableFuture.supplyAsync(() -> {
            crawlAllSitesInParallelWithLimit(playerName, jobId);
            return jobId;
        }, crawlerJobExecutor);
        
        ongoingCrawls.put(playerName, jobFuture);
        
        // 4. 작업 완료 시 ongoingCrawls에서 제거
        jobFuture.whenComplete((result, ex) -> {
            ongoingCrawls.remove(playerName);
            if (ex != null) {
                log.error("크롤링 작업 실패: {}", ex.getMessage());
                jobStatus.put(jobId, JobProgress.builder()
                        .status("FAILED")
                        .percentage(0)
                        .message("크롤링 작업 실패: " + ex.getMessage())
                        .build());
            }
        });

        return jobId;
    }

    public JobProgress getJobStatus(String jobId) {
        return jobStatus.getOrDefault(jobId, JobProgress.builder()
                .status("UNKNOWN")
                .percentage(0)
                .message("알 수 없는 작업입니다.")
                .build());
    }

    /**
     * Semaphore를 사용하여 동시 크롤링 작업 수 제한
     */
    public void crawlAllSitesInParallelWithLimit(String playerName, String jobId) {
        try {
            // Semaphore 획득 (동시 실행 제한)
            log.info("크롤링 작업 대기 중: '{}', Job ID: {}", playerName, jobId);
            boolean acquired = crawlerSemaphore.tryAcquire(30, TimeUnit.SECONDS);
            
            if (!acquired) {
                log.warn("크롤링 작업 시간 초과: '{}', Job ID: {}", playerName, jobId);
                jobStatus.put(jobId, JobProgress.builder()
                        .status("FAILED")
                        .percentage(0)
                        .message("서버가 혼잡합니다. 잠시 후 다시 시도해주세요.")
                        .build());
                return;
            }

            try {
                log.info("크롤링 작업 시작: '{}', Job ID: {}", playerName, jobId);
                jobStatus.put(jobId, JobProgress.builder()
                        .status("IN_PROGRESS")
                        .percentage(0)
                        .message("크롤링 준비 중...")
                        .build());
                
                crawlAllSitesInParallel(playerName, jobId);
            } finally {
                // Semaphore 해제
                crawlerSemaphore.release();
                log.info("크롤링 작업 완료 및 리소스 해제: '{}', Job ID: {}", playerName, jobId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("크롤링 작업 인터럽트: '{}', Job ID: {}", playerName, jobId, e);
            jobStatus.put(jobId, JobProgress.builder()
                    .status("FAILED")
                    .percentage(0)
                    .message("크롤링 작업이 중단되었습니다.")
                    .build());
        }
    }

    /**
     * 실제 크롤링 로직 (기존 메서드 유지)
     */
    public void crawlAllSitesInParallel(String playerName, String jobId) {
        log.info("비동기 병렬 크롤링 시작: '{}', Job ID: {}", playerName, jobId);
        Player player = playerRepository.findByName(playerName)
                .orElseGet(() -> playerRepository.save(Player.builder().name(playerName).build()));

        AtomicInteger completedCount = new AtomicInteger(0);
        int totalCrawlers = crawlers.size();

        if (totalCrawlers == 0) {
            jobStatus.put(jobId, JobProgress.builder()
                    .status("COMPLETED")
                    .percentage(100)
                    .message("완료되었습니다.")
                    .build());
            log.info("크롤링 대상 사이트가 없습니다: '{}', Job ID: {}", playerName, jobId);
            return;
        }

        List<CompletableFuture<Void>> futures = crawlers.stream()
                .map(crawler -> {
                    String message = String.format("%s 데이터 수집 중...", crawler.getSiteName());
                    int percentage = completedCount.get() * 100 / totalCrawlers;
                    jobStatus.put(jobId, JobProgress.builder()
                            .status("IN_PROGRESS")
                            .percentage(percentage)
                            .message(message)
                            .build());

                    return runSingleCrawlerAsync(crawler, player)
                            .whenComplete((ignored, ex) -> {
                                int finished = completedCount.incrementAndGet();
                                int finishedPercentage = finished * 100 / totalCrawlers;
                                String finishedMessage = String.format("%s 데이터 수집 완료 (%d/%d)",
                                        crawler.getSiteName(), finished, totalCrawlers);
                                jobStatus.put(jobId, JobProgress.builder()
                                        .status("IN_PROGRESS")
                                        .percentage(finishedPercentage)
                                        .message(finishedMessage)
                                        .build());
                            });
                })
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((ignored, ex) -> {
                    jobStatus.put(jobId, JobProgress.builder()
                            .status("COMPLETED")
                            .percentage(100)
                            .message("완료되었습니다.")
                            .build());
                    log.info("모든 비동기 병렬 크롤링 작업 완료: '{}', Job ID: {}", playerName, jobId);
                    
                    // 크롤링 완료 후 캐시 업데이트
                    try {
                        int recordCount = player.getAwardRecords().size();
                        String sitesScraped = crawlers.stream()
                                .map(SiteCrawler::getSiteName)
                                .collect(Collectors.joining(", "));
                        updateCache(playerName, recordCount, sitesScraped);
                    } catch (Exception e) {
                        log.error("캐시 업데이트 실패: {}", e.getMessage(), e);
                    }
                });
    }

    public CompletableFuture<Void> runSingleCrawlerAsync(SiteCrawler crawler, Player player) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("[{}] 사이트 크롤링 시작", crawler.getSiteName());
                crawler.scrape(player.getName());
                log.info("[{}] 사이트 크롤링 완료", crawler.getSiteName());
            } catch (Exception e) {
                log.error("[{}] 사이트 크롤링 중 오류 발생: {}", crawler.getSiteName(), e.getMessage(), e);
            }
        }, crawlerTaskExecutor);
    }
    
    /**
     * 캐싱된 선수 기록 조회 메서드
     * - 30분 동안 캐시 유지
     * - 캐시가 있으면 크롤링하지 않고 즉시 반환
     */
    @Cacheable(value = CacheConfig.PLAYER_RECORDS_CACHE, key = "#playerName")
    public String getCachedPlayerRecords(String playerName) {
        log.info("캐시 미스: '{}' - 크롤링 필요", playerName);
        // 이 메서드는 캐시가 없을 때만 호출됨
        // 실제 크롤링은 startCrawlingJob을 통해 수행
        return null;
    }
    
    /**
     * 크롤링 완료 후 캐시 업데이트
     */
    private void updateCache(String playerName, int recordCount, String sitesScraped) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(cacheDurationMinutes);

        Optional<CrawlingCache> existingCache = crawlingCacheRepository.findByPlayerName(playerName);
        
        if (existingCache.isPresent()) {
            // 기존 캐시 갱신
            CrawlingCache cache = existingCache.get();
            cache.refresh(cacheDurationMinutes, recordCount, sitesScraped);
            crawlingCacheRepository.save(cache);
            log.info("'{}' 선수 캐시 갱신 완료. 만료시간: {}", playerName, expiresAt);
        } else {
            // 새 캐시 생성
            CrawlingCache newCache = CrawlingCache.builder()
                    .playerName(playerName)
                    .lastCrawledAt(now)
                    .expiresAt(expiresAt)
                    .recordCount(recordCount)
                    .sitesScraped(sitesScraped)
                    .build();
            crawlingCacheRepository.save(newCache);
            log.info("'{}' 선수 새 캐시 생성 완료. 만료시간: {}", playerName, expiresAt);
        }
    }
}
