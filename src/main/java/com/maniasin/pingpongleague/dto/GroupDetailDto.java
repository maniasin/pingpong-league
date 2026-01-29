package com.maniasin.pingpongleague.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class GroupDetailDto {
    private String groupName;
    private List<MatchDetailDto> matches; // MODIFIED: Match -> MatchDetailDto
    private List<GroupStandingDto> standings;
    private List<GridPlayerDto> gridPlayers;
    private Map<String, MatchDetailDto> gridMatchesMap; // MODIFIED: Match -> MatchDetailDto
    private boolean isFinished;

    // MODIFIED: 생성자의 매개변수 타입 변경
    public GroupDetailDto(String groupName, List<MatchDetailDto> matches, List<GroupStandingDto> standings, List<GridPlayerDto> gridPlayers, Map<String, MatchDetailDto> gridMatchesMap, boolean isFinished) {
        this.groupName = groupName;
        this.matches = matches;
        this.standings = standings;
        this.gridPlayers = gridPlayers;
        this.gridMatchesMap = gridMatchesMap;
        this.isFinished = isFinished;
    }
}
