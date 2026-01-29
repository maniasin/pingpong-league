package com.maniasin.pingpongleague.repository;

import com.maniasin.pingpongleague.domain.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    // 대회명과 개최일로 대회를 찾는 메소드 (중복 저장을 방지하기 위해 사용)
    Optional<Tournament> findByNameAndTournamentDate(String name, LocalDate tournamentDate);
}