package com.maniasin.pingpongleague.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GridPlayerDto {
    private Long id;
    private String nickname;

    public GridPlayerDto(Long id, String nickname) {
        this.id = id;
        this.nickname = nickname;
    }
}
