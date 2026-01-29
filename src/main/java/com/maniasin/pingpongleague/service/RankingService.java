package com.maniasin.pingpongleague.service;

import com.maniasin.pingpongleague.domain.Match;
import com.maniasin.pingpongleague.domain.User;
import com.maniasin.pingpongleague.dto.GroupStandingDto;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RankingService {

    /**
     * 조별리그 순위를 계산하는 최종 로직
     */
    public List<GroupStandingDto> calculateGroupStandings(List<Match> groupMatches) {
        return calculateGroupStandings(groupMatches, com.maniasin.pingpongleague.domain.LeagueRoom.RoundRobinRankingType.POINTS);
    }

    public List<GroupStandingDto> calculateGroupStandings(List<Match> groupMatches, com.maniasin.pingpongleague.domain.LeagueRoom.RoundRobinRankingType rankingType) {
        if (groupMatches.isEmpty()) {
            return List.of();
        }

        // 1. 조에 속한 모든 선수 목록 및 기본 전적 계산
        Map<Long, GroupStandingDto> standingsMap = calculateInitialStandings(groupMatches);

        // 2. 승점/승수를 기준으로 동점자 그룹 생성
        Map<Integer, List<GroupStandingDto>> groupByScore = standingsMap.values().stream()
                .collect(Collectors.groupingBy(s -> rankingType == com.maniasin.pingpongleague.domain.LeagueRoom.RoundRobinRankingType.WINS
                        ? s.getWins()
                        : s.getPoints()));

        List<GroupStandingDto> finalStandings = new ArrayList<>();

        // 3. 점수가 높은 그룹부터 순회하며 동점자 처리
        groupByScore.entrySet().stream()
                .sorted(Map.Entry.<Integer, List<GroupStandingDto>>comparingByKey().reversed())
                .forEach(entry -> {
                    List<GroupStandingDto> tiedPlayers = entry.getValue();

                    // 동점자 그룹 정렬
                    sortTiedPlayers(tiedPlayers, groupMatches);

                    finalStandings.addAll(tiedPlayers);
                });

        // 4. 최종 순위 부여
        for (int i = 0; i < finalStandings.size(); i++) {
            finalStandings.get(i).setRank(i + 1);
        }

        return finalStandings;
    }

    /**
     * 1단계: 모든 경기를 바탕으로 선수별 기본 전적(승점, 세트 득실 등)을 계산
     */
    private Map<Long, GroupStandingDto> calculateInitialStandings(List<Match> groupMatches) {
        boolean isTeamMatch = groupMatches.stream()
                .anyMatch(match -> match.getTeam1() != null || match.getTeam2() != null);

        if (isTeamMatch) {
            List<com.maniasin.pingpongleague.domain.LeagueTeam> teamsInGroup = groupMatches.stream()
                    .flatMap(match -> Stream.of(match.getTeam1(), match.getTeam2()))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            Map<Long, GroupStandingDto> standingsMap = teamsInGroup.stream()
                    .collect(Collectors.toMap(com.maniasin.pingpongleague.domain.LeagueTeam::getId,
                            team -> new GroupStandingDto(team.getId(), team.getName())));

            groupMatches.stream()
                    .filter(match -> match.getStatus() == Match.MatchStatus.COMPLETED)
                    .forEach(match -> {
                        if (match.getWinnerTeam() == null || match.getTeam1() == null || match.getTeam2() == null) return;
                        com.maniasin.pingpongleague.domain.LeagueTeam winner = match.getWinnerTeam();
                        com.maniasin.pingpongleague.domain.LeagueTeam loser = winner.getId().equals(match.getTeam1().getId()) ? match.getTeam2() : match.getTeam1();

                        int winnerGames = winner.getId().equals(match.getTeam1().getId()) ? match.getPlayer1Score() : match.getPlayer2Score();
                        int loserGames = loser.getId().equals(match.getTeam1().getId()) ? match.getPlayer1Score() : match.getPlayer2Score();

                        standingsMap.get(winner.getId()).recordWin(winnerGames, loserGames);
                        standingsMap.get(loser.getId()).recordLoss(loserGames, winnerGames);
                    });
            return standingsMap;
        }

        List<User> playersInGroup = groupMatches.stream()
                .flatMap(match -> Stream.of(match.getPlayer1(), match.getPlayer2()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, GroupStandingDto> standingsMap = playersInGroup.stream()
                .collect(Collectors.toMap(User::getId, GroupStandingDto::new));

        groupMatches.stream()
                .filter(match -> match.getStatus() == Match.MatchStatus.COMPLETED)
                .forEach(match -> {
                    User winner = match.getWinner();
                    if (winner == null || match.getPlayer1() == null || match.getPlayer2() == null) return;
                    User loser = winner.getId().equals(match.getPlayer1().getId()) ? match.getPlayer2() : match.getPlayer1();

                    int winnerGames = winner.getId().equals(match.getPlayer1().getId()) ? match.getPlayer1Score() : match.getPlayer2Score();
                    int loserGames = loser.getId().equals(match.getPlayer1().getId()) ? match.getPlayer1Score() : match.getPlayer2Score();

                    standingsMap.get(winner.getId()).recordWin(winnerGames, loserGames);
                    standingsMap.get(loser.getId()).recordLoss(loserGames, winnerGames);
                });
        return standingsMap;
    }

    /**
     * 2단계: 동점자 그룹을 규칙에 맞게 정렬
     */
    private void sortTiedPlayers(List<GroupStandingDto> tiedPlayers, List<Match> allGroupMatches) {
        if (tiedPlayers.size() <= 1) {
            return; // 동점자가 아니면 정렬 불필요
        }

        if (tiedPlayers.size() == 2) {
            // 2명 동점: 승자승 규칙 적용
            GroupStandingDto p1 = tiedPlayers.get(0);
            GroupStandingDto p2 = tiedPlayers.get(1);
            Match headToHeadMatch = findHeadToHeadMatch(allGroupMatches, p1.getPlayerId(), p2.getPlayerId());

            if (headToHeadMatch != null && headToHeadMatch.getWinner() != null) {
                if (headToHeadMatch.getWinner().getId().equals(p2.getPlayerId())) {
                    Collections.swap(tiedPlayers, 0, 1); // p2가 이겼으면 순서를 바꿈
                    p2.setRemarks(p1.getPlayerName() + " 상대 승자승");
                } else {
                    p1.setRemarks(p2.getPlayerName() + " 상대 승자승");
                }
            }
        } else {
            // 3명 이상 동점: 동점자 간의 경기만 추려서 세트 득실률 계산
            List<Long> tiedPlayerIds = tiedPlayers.stream().map(GroupStandingDto::getPlayerId).toList();
            List<Match> tiedMatches = allGroupMatches.stream()
                    .filter(m -> m.getStatus() == Match.MatchStatus.COMPLETED &&
                            tiedPlayerIds.contains(m.getPlayer1().getId()) &&
                            tiedPlayerIds.contains(m.getPlayer2().getId()))
                    .toList();

            tiedPlayers.forEach(p -> {
                p.setRemarks("동점자 간 세트 득실");
                for (Match m : tiedMatches) {
                    if (m.getPlayer1().getId().equals(p.getPlayerId())) { // p가 player1일 때
                        p.recordTieBreakerMatch(m.getWinner().getId().equals(p.getPlayerId()), m.getPlayer1Score(), m.getPlayer2Score());
                    } else if (m.getPlayer2().getId().equals(p.getPlayerId())) { // p가 player2일 때
                        p.recordTieBreakerMatch(m.getWinner().getId().equals(p.getPlayerId()), m.getPlayer2Score(), m.getPlayer1Score());
                    }
                }
            });

            // 동점자 그룹 내부에서 세트 득실률 순으로 정렬
            tiedPlayers.sort(Comparator.comparing(GroupStandingDto::getTieBreakerGameRatio).reversed());
        }
    }

    // 두 선수 간의 경기를 찾는 헬퍼 메소드
    private Match findHeadToHeadMatch(List<Match> matches, Long p1Id, Long p2Id) {
        return matches.stream()
                .filter(m -> {
                    if (m.getTeam1() != null || m.getTeam2() != null) {
                        if (m.getTeam1() == null || m.getTeam2() == null) return false;
                        return (m.getTeam1().getId().equals(p1Id) && m.getTeam2().getId().equals(p2Id)) ||
                                (m.getTeam1().getId().equals(p2Id) && m.getTeam2().getId().equals(p1Id));
                    }
                    if (m.getPlayer1() == null || m.getPlayer2() == null) return false;
                    return (m.getPlayer1().getId().equals(p1Id) && m.getPlayer2().getId().equals(p2Id)) ||
                            (m.getPlayer1().getId().equals(p2Id) && m.getPlayer2().getId().equals(p1Id));
                })
                .findFirst()
                .orElse(null);
    }
}
