package com.maniasin.pingpongleague.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "award_records")
public class AwardRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player; // 입상한 선수

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament; // 참가한 대회

    @Column(nullable = false)
    private String division; // 참가 부수 (예: 남자 1부, 여자 희망부)

    @Column(nullable = false)
    private String detail; // 상세 부수 (예: 지역 혼성 7~8부)

    @Column(nullable = false)
    private String placing; // 성적 (예: 우승, 준우승)

    @Builder
    public AwardRecord(Player player, Tournament tournament, String division, String detail, String placing) {
        this.player = player;
        this.tournament = tournament;
        this.division = division;
        this.detail = detail;
        this.placing = placing;
    }
}