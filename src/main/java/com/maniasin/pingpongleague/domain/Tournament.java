package com.maniasin.pingpongleague.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tournaments")
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tournament_id")
    private Long id;

    @Column(nullable = false)
    private String name; // 대회명

    @Column(nullable = false)
    private LocalDate tournamentDate; // 개최일

    private String organizer; // 주최측

    @Builder
    public Tournament(String name, LocalDate tournamentDate, String organizer) {
        this.name = name;
        this.tournamentDate = tournamentDate;
        this.organizer = organizer;
    }
}