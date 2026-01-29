package com.maniasin.pingpongleague.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "league_groups")
public class LeagueGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private LeagueRoom leagueRoom;

    @Column(nullable = false)
    private String groupName; // 예: "A조", "B조"

    @Builder
    public LeagueGroup(LeagueRoom leagueRoom, String groupName) {
        this.leagueRoom = leagueRoom;
        this.groupName = groupName;
    }
}