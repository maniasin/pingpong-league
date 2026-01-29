package com.maniasin.pingpongleague.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 크롤링 캐시 데이터를 DB에 저장하는 엔티티
 * - 선수별 마지막 크롤링 시간 및 결과 저장
 * - 유효기간 체크하여 재크롤링 필요 여부 판단
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "crawling_cache", indexes = {
    @Index(name = "idx_player_name", columnList = "playerName"),
    @Index(name = "idx_last_crawled", columnList = "lastCrawledAt")
})
public class CrawlingCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cache_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String playerName;

    @Column(nullable = false)
    private LocalDateTime lastCrawledAt; // 마지막 크롤링 시간

    @Column(nullable = false)
    private LocalDateTime expiresAt; // 캐시 만료 시간

    @Column(nullable = false)
    private int recordCount; // 수집된 기록 수

    @Column(length = 1000)
    private String sitesScraped; // 크롤링한 사이트 목록 (콤마 구분)

    @Builder
    public CrawlingCache(String playerName, LocalDateTime lastCrawledAt, 
                        LocalDateTime expiresAt, int recordCount, String sitesScraped) {
        this.playerName = playerName;
        this.lastCrawledAt = lastCrawledAt;
        this.expiresAt = expiresAt;
        this.recordCount = recordCount;
        this.sitesScraped = sitesScraped;
    }

    /**
     * 캐시 갱신
     */
    public void refresh(int cacheDurationMinutes, int newRecordCount, String newSitesScraped) {
        this.lastCrawledAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(cacheDurationMinutes);
        this.recordCount = newRecordCount;
        this.sitesScraped = newSitesScraped;
    }

    /**
     * 캐시가 유효한지 체크
     */
    public boolean isValid() {
        return LocalDateTime.now().isBefore(expiresAt);
    }
}
