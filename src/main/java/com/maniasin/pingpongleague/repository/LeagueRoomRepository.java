package com.maniasin.pingpongleague.repository;

import com.maniasin.pingpongleague.domain.LeagueRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeagueRoomRepository extends JpaRepository<LeagueRoom, Long> {

    // 이 메소드가 추가되었습니다.
    @Query("SELECT lr FROM LeagueRoom lr JOIN FETCH lr.owner")
    List<LeagueRoom> findAllWithOwner();

    // ▼▼▼ 이 메소드를 추가하거나, 기존 findById를 이 내용으로 교체해주세요. ▼▼▼
    @Query("SELECT lr FROM LeagueRoom lr " +
            "LEFT JOIN FETCH lr.owner " +
            "LEFT JOIN FETCH lr.participants p " +
            "LEFT JOIN FETCH p.user " +
            "WHERE lr.id = :roomId")
    Optional<LeagueRoom> findByIdWithDetails(@Param("roomId") Long roomId);
}