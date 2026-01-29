package com.maniasin.pingpongleague.repository;

import com.maniasin.pingpongleague.domain.LeagueRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface LeagueRoomParticipantRepository extends JpaRepository<LeagueRoomParticipant, Long> {
    @Query("SELECT p FROM LeagueRoomParticipant p JOIN FETCH p.user WHERE p.leagueRoom.id = :leagueRoomId AND p.user.id = :userId")
    Optional<LeagueRoomParticipant> findByLeagueRoomIdAndUserId(@Param("leagueRoomId") Long leagueRoomId, @Param("userId") Long userId);
    int countByLeagueRoomId(Long leagueRoomId);
    List<LeagueRoomParticipant> findByLeagueRoomId(Long leagueRoomId);
}
