package com.maniasin.pingpongleague.dto;

import com.maniasin.pingpongleague.domain.LeagueRoom;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LeagueRoomUpdateRequestDto {
    private String title;
    private int maxParticipants;
    private int playersPerGroup;
    private int advancingPlayersPerGroup; // <--- 이 부분 수정 ---
    private LeagueRoom.TournamentType tournamentType;
    private LeagueRoom.RoundRobinRankingType roundRobinRankingType;
    private Integer teamSize;
    private LeagueRoom.TeamMatchFormat teamMatchFormat;
    private String location;
    private String venueAddress;
    private String matchDescription;
    private String contactInfo;
    private String eventDate;
    private String eventTime;
}
