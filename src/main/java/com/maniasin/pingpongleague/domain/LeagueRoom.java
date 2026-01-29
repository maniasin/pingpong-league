package com.maniasin.pingpongleague.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.maniasin.pingpongleague.dto.LeagueRoomUpdateRequestDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "league_rooms")
public class LeagueRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @Setter
    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Setter
    @Column(nullable = false)
    private int maxParticipants;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameType gameType;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchFormat matchFormat;

    @Setter
    @Enumerated(EnumType.STRING)
    private RoundRobinRankingType roundRobinRankingType;

    @Setter
    @Enumerated(EnumType.STRING)
    private TournamentType tournamentType;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable =false)
    private RoomStatus status;

    @Setter
    private Integer teamSize;

    @Setter
    @Enumerated(EnumType.STRING)
    private TeamMatchFormat teamMatchFormat;

    @Setter
    @Column(nullable = false)
    private String location;

    @Setter
    private String venueAddress;

    @Setter
    @Column(length = 1000)
    private String matchDescription;

    @Setter
    private String contactInfo;

    @Setter
    private int playersPerGroup;

    // <--- 이 부분 수정 시작 ---
    @Setter
    private int advancingPlayersPerGroup; // 조별 본선 진출 인원
    // <--- 이 부분 수정 끝 ---

    @Setter
    private LocalDateTime eventDateTime;

    @OneToMany(mappedBy = "leagueRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeagueRoomParticipant> participants = new ArrayList<>();

    @Builder
    public LeagueRoom(String title, User owner, int maxParticipants, GameType gameType, MatchFormat matchFormat, RoundRobinRankingType roundRobinRankingType, TournamentType tournamentType, RoomStatus status, Integer teamSize, TeamMatchFormat teamMatchFormat, String location, String venueAddress, String matchDescription, String contactInfo, int playersPerGroup, LocalDateTime eventDateTime, int advancingPlayersPerGroup) {
        this.title = title;
        this.owner = owner;
        this.maxParticipants = maxParticipants;
        this.gameType = gameType;
        this.matchFormat = matchFormat;
        this.roundRobinRankingType = roundRobinRankingType;
        this.tournamentType = tournamentType;
        this.status = status;
        this.teamSize = teamSize;
        this.teamMatchFormat = teamMatchFormat;
        this.location = location;
        this.venueAddress = venueAddress;
        this.matchDescription = matchDescription;
        this.contactInfo = contactInfo;
        this.playersPerGroup = playersPerGroup;
        this.eventDateTime = eventDateTime;
        this.advancingPlayersPerGroup = advancingPlayersPerGroup; // <--- 이 부분 수정 ---
    }

    public void update(LeagueRoomUpdateRequestDto requestDto) {
        this.title = requestDto.getTitle();
        this.maxParticipants = requestDto.getMaxParticipants();
        this.location = requestDto.getLocation();
        this.venueAddress = requestDto.getVenueAddress();
        this.matchDescription = requestDto.getMatchDescription();
        this.contactInfo = requestDto.getContactInfo();
        this.playersPerGroup = requestDto.getPlayersPerGroup();
        this.advancingPlayersPerGroup = requestDto.getAdvancingPlayersPerGroup(); // <--- 이 부분 수정 ---
        if (requestDto.getRoundRobinRankingType() != null) {
            this.roundRobinRankingType = requestDto.getRoundRobinRankingType();
        }
        if (requestDto.getTeamSize() != null) {
            this.teamSize = requestDto.getTeamSize();
        }
        if (requestDto.getTeamMatchFormat() != null) {
            this.teamMatchFormat = requestDto.getTeamMatchFormat();
        }
        this.tournamentType = requestDto.getTournamentType();
        this.eventDateTime = LocalDateTime.of(
                LocalDate.parse(requestDto.getEventDate()),
                LocalTime.parse(requestDto.getEventTime())
        );
    }

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    public enum GameType {
        SINGLE("단식"), DOUBLE("복식"), TEAM("단체전");
        private final String displayName;
        GameType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    public enum MatchFormat {
        ROUND_ROBIN("조별 풀리그"), PRELIMINARY_TOURNAMENT("예선+본선 토너먼트");
        private final String displayName;
        MatchFormat(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    public enum TeamMatchFormat {
        SINGLES_ONLY("단식만"), SINGLE_DOUBLE_SINGLE("단복단");
        private final String displayName;
        TeamMatchFormat(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    public enum RoundRobinRankingType {
        WINS("승수 방식"), POINTS("승점 방식");
        private final String displayName;
        RoundRobinRankingType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    public enum TournamentType {
        STANDARD("단방향 (순차 진행)"), SPLIT("양방향 (좌우 대칭)");
        private final String displayName;
        TournamentType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    public enum RoomStatus {
        OPEN("모집중"), CLOSED("모집마감"), IN_PROGRESS("진행중"), COMPLETED("종료");
        private final String displayName;
        RoomStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
}
