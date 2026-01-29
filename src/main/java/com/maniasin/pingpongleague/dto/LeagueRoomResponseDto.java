package com.maniasin.pingpongleague.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.maniasin.pingpongleague.domain.LeagueRoom;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class LeagueRoomResponseDto {
    private Long id;
    private String title;
    private String ownerUsername;
    private int maxParticipants;
    private int currentParticipants;
    private int playersPerGroup;
    private int advancingPlayersPerGroup; // <--- 이 부분 수정 ---
    private LeagueRoom.GameType gameType;
    private LeagueRoom.MatchFormat matchFormat;
    private LeagueRoom.RoundRobinRankingType roundRobinRankingType;
    private LeagueRoom.TournamentType tournamentType; // 토너먼트 타입 추가
    private LeagueRoom.RoomStatus status;
    private String location;
    private Integer teamSize;
    private LeagueRoom.TeamMatchFormat teamMatchFormat;
    private String venueAddress;
    private String matchDescription;
    private String contactInfo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime eventDateTime;

    private List<String> participants;

    public LeagueRoomResponseDto(LeagueRoom leagueRoom) {
        this.id = leagueRoom.getId();
        this.title = leagueRoom.getTitle();
        this.ownerUsername = leagueRoom.getOwner().getUsername();
        this.maxParticipants = leagueRoom.getMaxParticipants();
        this.currentParticipants = leagueRoom.getParticipants().size();
        this.playersPerGroup = leagueRoom.getPlayersPerGroup();
        this.advancingPlayersPerGroup = leagueRoom.getAdvancingPlayersPerGroup();
        this.gameType = leagueRoom.getGameType();
        this.matchFormat = leagueRoom.getMatchFormat();
        this.roundRobinRankingType = leagueRoom.getRoundRobinRankingType();
        this.tournamentType = leagueRoom.getTournamentType(); // 토너먼트 타입 설정
        this.status = leagueRoom.getStatus();
        this.location = leagueRoom.getLocation();
        this.teamSize = leagueRoom.getTeamSize();
        this.teamMatchFormat = leagueRoom.getTeamMatchFormat();
        this.venueAddress = leagueRoom.getVenueAddress();
        this.matchDescription = leagueRoom.getMatchDescription();
        this.contactInfo = leagueRoom.getContactInfo();
        this.eventDateTime = leagueRoom.getEventDateTime();
        this.participants = leagueRoom.getParticipants().stream()
                .map(participant -> participant.getUser().getUsername()) // getUsername()으로 변경
                .collect(Collectors.toList());
    }
}
