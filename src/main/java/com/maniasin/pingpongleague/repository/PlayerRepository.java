package com.maniasin.pingpongleague.repository;

import com.maniasin.pingpongleague.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    // 이름으로 선수를 찾는 메소드 (중복 저장을 방지하기 위해 사용)
    Optional<Player> findByName(String name);
}