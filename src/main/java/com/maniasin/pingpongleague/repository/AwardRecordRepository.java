package com.maniasin.pingpongleague.repository;

import com.maniasin.pingpongleague.domain.AwardRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AwardRecordRepository extends JpaRepository<AwardRecord, Long> {
    @Query("SELECT ar FROM AwardRecord ar JOIN FETCH ar.tournament t WHERE ar.player.id = :playerId ORDER BY t.tournamentDate DESC")
    List<AwardRecord> findByPlayerIdWithTournament(@Param("playerId") Long playerId);

    boolean existsByPlayerIdAndTournamentIdAndDivisionAndPlacing(Long playerId, Long tournamentId, String division, String placing);
}