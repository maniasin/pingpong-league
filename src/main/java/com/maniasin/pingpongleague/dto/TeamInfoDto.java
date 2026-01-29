package com.maniasin.pingpongleague.dto;

import com.maniasin.pingpongleague.domain.LeagueTeam;
import com.maniasin.pingpongleague.domain.LeagueTeamMember;
import lombok.Getter;

import java.util.List;

@Getter
public class TeamInfoDto {
    private Long id;
    private String name;
    private List<Long> memberIds;

    public TeamInfoDto(LeagueTeam team, List<LeagueTeamMember> members) {
        this.id = team.getId();
        this.name = team.getName();
        this.memberIds = members.stream().map(m -> m.getUser().getId()).toList();
    }
}
