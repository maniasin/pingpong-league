package com.maniasin.pingpongleague.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MatchResultDto {
    private Long matchId;
    private int player1Score;
    private int player2Score;
}