package com.maniasin.pingpongleague.repository;

import com.maniasin.pingpongleague.domain.LeagueTeam;
import com.maniasin.pingpongleague.domain.LeagueTeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeagueTeamMemberRepository extends JpaRepository<LeagueTeamMember, Long> {
    List<LeagueTeamMember> findByTeam(LeagueTeam team);
    void deleteByTeam(LeagueTeam team);
}
