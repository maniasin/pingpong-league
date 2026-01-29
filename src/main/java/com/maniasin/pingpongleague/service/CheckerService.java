package com.maniasin.pingpongleague.service;

import com.maniasin.pingpongleague.domain.AwardRecord;
import com.maniasin.pingpongleague.domain.CrawlingCache;
import com.maniasin.pingpongleague.repository.AwardRecordRepository;
import com.maniasin.pingpongleague.repository.CrawlingCacheRepository;
import com.maniasin.pingpongleague.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CheckerService {

    private final PlayerRepository playerRepository;
    private final AwardRecordRepository awardRecordRepository;
    private final CrawlingCacheRepository crawlingCacheRepository;
    private final CrawlerService crawlerService; // 크롤링은 CrawlerService에 위임

    @Value("${crawler.cache-duration-minutes:30}")
    private int cacheDurationMinutes;

    /**
     * 선수 기록 조회 (캐시 체크 포함)
     * - 캐시가 유효하면 DB에서 바로 조회
     * - 캐시가 없거나 만료되었으면 크롤링 필요 표시
     */
    @Transactional(readOnly = true)
    public List<AwardRecord> getExistingRecords(String playerName) {
        log.info("'{}' 선수 최종 기록 조회", playerName);
        return playerRepository.findByName(playerName)
                .map(player -> awardRecordRepository.findByPlayerIdWithTournament(player.getId()))
                .orElse(Collections.emptyList());
    }

    /**
     * 캐시 유효성 체크
     * @return true if cache is valid and fresh
     */
    @Transactional(readOnly = true)
    public boolean isCacheValid(String playerName) {
        Optional<CrawlingCache> cache = crawlingCacheRepository.findByPlayerName(playerName);
        boolean valid = cache.isPresent() && cache.get().isValid();
        log.info("'{}' 선수 캐시 상태: {}", playerName, valid ? "유효" : "만료 또는 없음");
        return valid;
    }

    /**
     * 크롤링 완료 후 캐시 업데이트
     */
    @Transactional
    public void updateCache(String playerName, int recordCount, String sitesScraped) {
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

    /**
     * 만료된 캐시 일괄 삭제
     */
    @Transactional
    public void cleanupExpiredCaches() {
        crawlingCacheRepository.deleteExpiredCaches(LocalDateTime.now());
        log.info("만료된 캐시 정리 완료");
    }
}
