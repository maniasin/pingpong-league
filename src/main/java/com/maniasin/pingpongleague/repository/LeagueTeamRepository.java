package com.maniasin.pingpongleague.repository;

import com.maniasin.pingpongleague.domain.LeagueRoom;
import com.maniasin.pingpongleague.domain.LeagueTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeagueTeamRepository extends JpaRepository<LeagueTeam, Long> {
    List<LeagueTeam> findByLeagueRoom(LeagueRoom leagueRoom);
    void deleteByLeagueRoom(LeagueRoom leagueRoom);
}
