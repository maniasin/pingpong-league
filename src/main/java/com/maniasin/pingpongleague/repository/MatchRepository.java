package com.maniasin.pingpongleague.repository;

import com.maniasin.pingpongleague.domain.LeagueRoom;
import com.maniasin.pingpongleague.domain.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    void deleteByLeagueRoom(LeagueRoom leagueRoom);
    boolean existsByLeagueRoomAndRoundNumberAndStatus(LeagueRoom leagueRoom, int roundNumber, Match.MatchStatus status);
    List<Match> findByLeagueRoomAndRoundNumber(LeagueRoom leagueRoom, int lastRound);
    boolean existsByLeagueRoomAndStatus(LeagueRoom leagueRoom, Match.MatchStatus status);

    @Query("SELECT MAX(m.roundNumber) FROM Match m WHERE m.leagueRoom = :leagueRoom")
    Optional<Integer> getLatestRoundNumber(@Param("leagueRoom") LeagueRoom leagueRoom);

    @Query("SELECT MAX(m.roundNumber) FROM Match m WHERE m.leagueRoom = :leagueRoom AND m.leagueGroup IS NULL")
    Optional<Integer> findLatestTournamentRoundNumber(@Param("leagueRoom") LeagueRoom leagueRoom);

    // 디버깅용: 특정 라운드에서 PENDING 상태인 경기를 모두 찾음
    List<Match> findByLeagueRoomAndRoundNumberAndStatus(LeagueRoom leagueRoom, int roundNumber, Match.MatchStatus status);

    // LEFT JOIN FETCH를 사용하여 leagueGroup이 null인 본선 경기도 함께 조회합니다.
    @Query("SELECT m FROM Match m " +
            "LEFT JOIN FETCH m.leagueGroup " +
            "LEFT JOIN FETCH m.player1 " +
            "LEFT JOIN FETCH m.player2 " +
            "LEFT JOIN FETCH m.team1 " +
            "LEFT JOIN FETCH m.team2 " +
            "LEFT JOIN FETCH m.winnerTeam " +
            "WHERE m.leagueRoom = :leagueRoom")
    List<Match> findByLeagueRoomWithDetails(@Param("leagueRoom") LeagueRoom leagueRoom);

    /**
     * 본선 토너먼트의 특정 라운드 경기만 조회하는 쿼리
     * leagueGroup이 NULL인 조건으로 예선전과 완벽하게 분리합니다.
     */
    @Query("SELECT m FROM Match m WHERE m.leagueRoom = :leagueRoom AND m.leagueGroup IS NULL AND m.roundNumber = :roundNumber")
    List<Match> findTournamentMatchesByRound(@Param("leagueRoom") LeagueRoom leagueRoom, @Param("roundNumber") int roundNumber);
}
