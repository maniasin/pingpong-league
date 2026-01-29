package com.maniasin.pingpongleague.service;

import com.maniasin.pingpongleague.domain.User;
import com.maniasin.pingpongleague.dto.UserSearchDto;
import com.maniasin.pingpongleague.dto.UserSignUpRequestDto;
import com.maniasin.pingpongleague.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signUp(UserSignUpRequestDto requestDto) {

        if (!requestDto.getPassword().equals(requestDto.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        if (userRepository.findByUsername(requestDto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByNickname(requestDto.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        if (userRepository.existsByPhone(requestDto.getPhone())) {
            throw new IllegalArgumentException("이미 등록된 연락처입니다.");
        }

        User user = User.builder()
                .username(requestDto.getUsername())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .nickname(requestDto.getNickname())
                .name(requestDto.getName())
                .phone(requestDto.getPhone())
                .build();

        User savedUser = userRepository.save(user);
        return savedUser.getId();
    }

    public List<UserSearchDto> searchUsers(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        String keyword = query.trim();
        return userRepository
                .findTop10ByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(user -> new UserSearchDto(user.getId(), user.getUsername(), user.getNickname()))
                .collect(Collectors.toList());
    }
}
