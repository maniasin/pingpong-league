package com.maniasin.pingpongleague.repository;

import com.maniasin.pingpongleague.domain.CrawlingCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CrawlingCacheRepository extends JpaRepository<CrawlingCache, Long> {
    
    Optional<CrawlingCache> findByPlayerName(String playerName);
    
    @Modifying
    @Query("DELETE FROM CrawlingCache c WHERE c.expiresAt < :now")
    void deleteExpiredCaches(LocalDateTime now);
}
