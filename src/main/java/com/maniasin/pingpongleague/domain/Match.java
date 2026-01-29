package com.maniasin.pingpongleague.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private LeagueRoom leagueRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private LeagueGroup leagueGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player1_id")
    private User player1;

    // highlight-start
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player2_id") // nullable = false 제약조건을 제거하여 null을 허용
    private User player2;
    // highlight-end

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team1_id")
    private LeagueTeam team1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team2_id")
    private LeagueTeam team2;

    @Column(nullable = false)
    private int roundNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    private Integer player1Score;

    private Integer player2Score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_team_id")
    private LeagueTeam winnerTeam;

    @Builder
    public Match(LeagueRoom leagueRoom, LeagueGroup leagueGroup, User player1, User player2, LeagueTeam team1, LeagueTeam team2, int roundNumber, MatchStatus status) {
        this.leagueRoom = leagueRoom;
        this.leagueGroup = leagueGroup;
        this.player1 = player1;
        this.player2 = player2;
        this.team1 = team1;
        this.team2 = team2;
        this.roundNumber = roundNumber;
        this.status = status;
    }

    public enum MatchStatus {
        PENDING("대기중"),
        COMPLETED("경기종료");

        private final String displayName;
        MatchStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
}
