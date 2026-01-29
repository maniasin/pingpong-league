package com.maniasin.pingpongleague.dto;

import com.maniasin.pingpongleague.domain.Match; // Match 엔티티 참조를 위해 필요
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MatchDetailDto {
    private Long id;
    private String player1Nickname; // 선수1 닉네임
    private Long player1Id; // 선수1 ID
    private String player2Nickname; // 선수2 닉네임
    private Long player2Id; // 선수2 ID
    private int roundNumber; // 라운드 번호
    private String status; // 경기 상태 (ENUM.name())
    private Integer player1Score; // 선수1 점수
    private Integer player2Score; // 선수2 점수
    private Long winnerId; // 승자 ID

    public MatchDetailDto(Match match) {
        this.id = match.getId();
        if (match.getTeam1() != null || match.getTeam2() != null) {
            this.player1Nickname = match.getTeam1() != null ? match.getTeam1().getName() : null;
            this.player1Id = match.getTeam1() != null ? match.getTeam1().getId() : null;
            this.player2Nickname = match.getTeam2() != null ? match.getTeam2().getName() : null;
            this.player2Id = match.getTeam2() != null ? match.getTeam2().getId() : null;
            this.winnerId = match.getWinnerTeam() != null ? match.getWinnerTeam().getId() : null;
        } else {
            this.player1Nickname = match.getPlayer1() != null ? match.getPlayer1().getNickname() : null; // 선수1 닉네임 설정
            this.player1Id = match.getPlayer1() != null ? match.getPlayer1().getId() : null; // 선수1 ID 설정
            this.player2Nickname = match.getPlayer2() != null ? match.getPlayer2().getNickname() : null; // 선수2 닉네임 설정
            this.player2Id = match.getPlayer2() != null ? match.getPlayer2().getId() : null; // 선수2 ID 설정
            this.winnerId = match.getWinner() != null ? match.getWinner().getId() : null; // 승자 ID 설정
        }
        this.roundNumber = match.getRoundNumber(); // 라운드 번호 설정
        this.status = match.getStatus().name(); // 경기 상태의 ENUM 이름 사용
        this.player1Score = match.getPlayer1Score(); // 선수1 점수 설정
        this.player2Score = match.getPlayer2Score(); // 선수2 점수 설정
    }
}
