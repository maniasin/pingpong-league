package com.maniasin.pingpongleague.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GridResultDto {
    private Long matchId;
    private Long rowPlayerId;
    private Long colPlayerId;
    private int rowPlayerScore;
    private int colPlayerScore;
}