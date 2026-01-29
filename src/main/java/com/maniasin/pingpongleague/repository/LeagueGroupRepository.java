package com.maniasin.pingpongleague.repository;

import com.maniasin.pingpongleague.domain.LeagueGroup;
import com.maniasin.pingpongleague.domain.LeagueRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueGroupRepository extends JpaRepository<LeagueGroup, Long> {
    void deleteByLeagueRoom(LeagueRoom leagueRoom);
}