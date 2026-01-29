package com.maniasin.pingpongleague.dto;

import com.maniasin.pingpongleague.domain.User;
import lombok.Getter;

@Getter
public class ParticipantDto {
    private Long id;
    private String nickname;

    public ParticipantDto(User user) {
        this.id = user.getId();
        this.nickname = user.getNickname();
    }
}