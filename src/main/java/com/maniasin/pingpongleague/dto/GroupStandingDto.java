package com.maniasin.pingpongleague.dto;

import com.maniasin.pingpongleague.domain.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupStandingDto {
    private int rank;
    private String playerName;
    private Long playerId;
    private int played;
    private int wins;
    private int losses;
    private int points;
    private int gamesWon;
    private int gamesLost;
    private String remarks;

    // 동점자 간의 전적을 계산하기 위한 필드
    private int tieBreakerGamesWon;
    private int tieBreakerGamesLost;

    public GroupStandingDto(User player) {
        this.playerName = player.getNickname();
        this.playerId = player.getId();
        this.played = 0;
        this.wins = 0;
        this.losses = 0;
        this.points = 0;
        this.gamesWon = 0;
        this.gamesLost = 0;
        this.remarks = "";
        this.tieBreakerGamesWon = 0;
        this.tieBreakerGamesLost = 0;
    }

    public GroupStandingDto(Long playerId, String playerName) {
        this.playerName = playerName;
        this.playerId = playerId;
        this.played = 0;
        this.wins = 0;
        this.losses = 0;
        this.points = 0;
        this.gamesWon = 0;
        this.gamesLost = 0;
        this.remarks = "";
        this.tieBreakerGamesWon = 0;
        this.tieBreakerGamesLost = 0;
    }

    public void recordWin(int gamesWon, int gamesLost) {
        this.played++;
        this.wins++;
        this.points += 3;
        this.gamesWon += gamesWon;
        this.gamesLost += gamesLost;
    }

    public void recordLoss(int gamesWon, int gamesLost) {
        this.played++;
        this.losses++;
        this.points += 1;
        this.gamesWon += gamesWon;
        this.gamesLost += gamesLost;
    }

    // 동점자 간 경기 득실 계산용 메소드
    public void recordTieBreakerMatch(boolean isWin, int gamesWon, int gamesLost) {
        if(isWin) this.tieBreakerGamesWon += gamesWon;
        else this.tieBreakerGamesLost += gamesLost;
    }

    // 세트 득실률 계산
    public double getGameRatio() {
        if (this.gamesLost == 0) {
            return this.gamesWon > 0 ? Double.POSITIVE_INFINITY : 0;
        }
        return (double) this.gamesWon / this.gamesLost;
    }

    // 동점자 간 세트 득실률 계산
    public double getTieBreakerGameRatio() {
        if (this.tieBreakerGamesLost == 0) {
            return this.tieBreakerGamesWon > 0 ? Double.POSITIVE_INFINITY : 0;
        }
        return (double) this.tieBreakerGamesWon / this.tieBreakerGamesLost;
    }
}
