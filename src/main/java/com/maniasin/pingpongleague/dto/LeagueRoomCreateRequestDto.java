package com.maniasin.pingpongleague.dto;

import com.maniasin.pingpongleague.domain.LeagueRoom;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LeagueRoomCreateRequestDto {
    private String title;
    private int maxParticipants;
    private LeagueRoom.GameType gameType;
    private LeagueRoom.MatchFormat matchFormat;
    private LeagueRoom.RoundRobinRankingType roundRobinRankingType;
    private int playersPerGroup;
    // <--- 이 부분 수정 시작 ---
    private int advancingPlayersPerGroup;
    private LeagueRoom.TournamentType tournamentType;
    // <--- 이 부분 수정 끝 ---
    private Integer teamSize;
    private LeagueRoom.TeamMatchFormat teamMatchFormat;
    private String location;
    private String venueAddress;
    private String matchDescription;
    private String contactInfo;
    private String eventDate;
    private String eventTime;
}
