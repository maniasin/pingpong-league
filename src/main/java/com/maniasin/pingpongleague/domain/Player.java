package com.maniasin.pingpongleague.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_id")
    private Long id;

    @Column(nullable = false)
    private String name; // 선수 이름

    private String region; // 소속 지역 (예: 서울, 경기)

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    private List<AwardRecord> awardRecords = new ArrayList<>();

    @Builder
    public Player(String name, String region) {
        this.name = name;
        this.region = region;
    }

    public void addAwardRecord(AwardRecord awardRecord) {
        this.awardRecords.add(awardRecord);
        awardRecord.setPlayer(this);
    }

}