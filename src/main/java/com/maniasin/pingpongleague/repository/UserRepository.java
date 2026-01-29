package com.maniasin.pingpongleague.repository;

import com.maniasin.pingpongleague.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByNickname(String nickname);
    boolean existsByNickname(String nickname);
    boolean existsByPhone(String phone);

    List<User> findTop10ByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCase(String username, String nickname);
}
