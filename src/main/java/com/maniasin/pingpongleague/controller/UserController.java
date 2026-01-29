package com.maniasin.pingpongleague.controller;

import com.maniasin.pingpongleague.dto.UserSearchDto;
import com.maniasin.pingpongleague.dto.UserSignUpRequestDto;
import com.maniasin.pingpongleague.service.UserService;
import jakarta.validation.Valid; // @Valid를 사용하기 위해 import 합니다.
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    // @Valid: requestDto 객체에 대해 DTO에 정의된 유효성 검사를 실행합니다.
    // 파라미터를 UserSignUpRequestDto 객체로 받도록 변경합니다.
    public ResponseEntity<Void> signUp(@Valid @RequestBody UserSignUpRequestDto requestDto) {
        Long userId = userService.signUp(requestDto);
        return ResponseEntity.created(URI.create("/api/users/" + userId)).build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchDto>> searchUsers(@RequestParam("query") String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
    }
}
