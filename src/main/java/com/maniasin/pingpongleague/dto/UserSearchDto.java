package com.maniasin.pingpongleague.dto;

import lombok.Getter;

@Getter
public class UserSearchDto {
    private Long id;
    private String username;
    private String nickname;

    public UserSearchDto(Long id, String username, String nickname) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
    }
}
