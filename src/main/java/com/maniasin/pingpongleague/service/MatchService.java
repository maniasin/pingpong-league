package com.maniasin.pingpongleague.service;

import com.maniasin.pingpongleague.domain.*;
import com.maniasin.pingpongleague.dto.FinalResultDto;
import com.maniasin.pingpongleague.dto.GridResultDto;
import com.maniasin.pingpongleague.dto.GroupStandingDto;
import com.maniasin.pingpongleague.dto.MatchResultDto;
import com.maniasin.pingpongleague.dto.TournamentDataDto;
import com.maniasin.pingpongleague.repository.LeagueGroupRepository;
import com.maniasin.pingpongleague.repository.LeagueRoomParticipantRepository;
import com.maniasin.pingpongleague.repository.LeagueRoomRepository;
import com.maniasin.pingpongleague.repository.LeagueTeamMemberRepository;
import com.maniasin.pingpongleague.repository.LeagueTeamRepository;
import com.maniasin.pingpongleague.repository.MatchRepository;
import com.maniasin.pingpongleague.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);
    private final MatchRepository matchRepository;
    private final LeagueRoomParticipantRepository leagueRoomParticipantRepository;
    private final LeagueRoomRepository leagueRoomRepository;
    private final LeagueGroupRepository leagueGroupRepository;
    private final LeagueTeamRepository leagueTeamRepository;
    private final LeagueTeamMemberRepository leagueTeamMemberRepository;
    private final RankingService rankingService;
    private final UserRepository userRepository;

    public void generateGroupStageMatches(LeagueRoom leagueRoom) {
        if (leagueRoom.getStatus() != LeagueRoom.RoomStatus.OPEN) {
            throw new IllegalStateException("이미 시작되었거나 종료된 리그입니다.");
        }
        List<LeagueRoomParticipant> participants = leagueRoom.getParticipants();
        Collections.shuffle(participants);
        int playersPerGroup = leagueRoom.getPlayersPerGroup();
        if (playersPerGroup <= 1) {
            throw new IllegalArgumentException("조별 인원수는 2 이상이어야 합니다.");
        }
        int groupCount;
        char groupNameChar = 'A';
        List<Match> allMatches = new ArrayList<>();

        if (leagueRoom.getGameType() == LeagueRoom.GameType.TEAM) {
            List<User> participantUsers = participants.stream()
                    .map(LeagueRoomParticipant::getUser)
                    .collect(Collectors.toList());
            List<LeagueTeam> teams = ensureTeams(leagueRoom, participantUsers);
            Collections.shuffle(teams);
            groupCount = (int) Math.ceil((double) teams.size() / playersPerGroup);

            for (int i = 0; i < groupCount; i++) {
                LeagueGroup group = LeagueGroup.builder()
                        .leagueRoom(leagueRoom)
                        .groupName((char) (groupNameChar + i) + "조")
                        .build();
                leagueGroupRepository.save(group);

                int start = i * playersPerGroup;
                int end = Math.min(start + playersPerGroup, teams.size());
                List<LeagueTeam> groupTeams = new ArrayList<>(teams.subList(start, end));
                if (groupTeams.size() < 2) continue;
                if (groupTeams.size() % 2 != 0) {
                    groupTeams.add(null);
                }

                int numTeams = groupTeams.size();
                int numRounds = numTeams - 1;
                List<LeagueTeam> rotatingTeams = new ArrayList<>(groupTeams);
                for (int round = 0; round < numRounds; round++) {
                    for (int matchIdx = 0; matchIdx < numTeams / 2; matchIdx++) {
                        LeagueTeam team1 = rotatingTeams.get(matchIdx);
                        LeagueTeam team2 = rotatingTeams.get(numTeams - 1 - matchIdx);
                        if (team1 != null && team2 != null) {
                            Match match = Match.builder()
                                    .leagueRoom(leagueRoom)
                                    .leagueGroup(group)
                                    .player1(null)
                                    .player2(null)
                                    .team1(team1)
                                    .team2(team2)
                                    .roundNumber(round + 1)
                                    .status(Match.MatchStatus.PENDING)
                                    .build();
                            allMatches.add(match);
                        }
                    }
                    LeagueTeam lastTeam = rotatingTeams.remove(numTeams - 1);
                    rotatingTeams.add(1, lastTeam);
                }
            }
        } else {
            groupCount = (int) Math.ceil((double) participants.size() / playersPerGroup);
            for (int i = 0; i < groupCount; i++) {
                LeagueGroup group = LeagueGroup.builder()
                        .leagueRoom(leagueRoom)
                        .groupName((char) (groupNameChar + i) + "조")
                        .build();
                leagueGroupRepository.save(group);
                int start = i * playersPerGroup;
                int end = Math.min(start + playersPerGroup, participants.size());
                List<User> groupPlayers = participants.subList(start, end).stream()
                        .map(LeagueRoomParticipant::getUser).collect(Collectors.toList());
                if (groupPlayers.size() < 2) continue;
                if (groupPlayers.size() % 2 != 0) {
                    groupPlayers.add(null);
                }
                int numPlayers = groupPlayers.size();
                int numRounds = numPlayers - 1;
                List<User> rotatingPlayers = new ArrayList<>(groupPlayers);
                for (int round = 0; round < numRounds; round++) {
                    for (int matchIdx = 0; matchIdx < numPlayers / 2; matchIdx++) {
                        User player1 = rotatingPlayers.get(matchIdx);
                        User player2 = rotatingPlayers.get(numPlayers - 1 - matchIdx);
                        if (player1 != null && player2 != null) {
                            Match match = Match.builder()
                                    .leagueRoom(leagueRoom)
                                    .leagueGroup(group)
                                    .player1(player1)
                                    .player2(player2)
                                    .team1(null)
                                    .team2(null)
                                    .roundNumber(round + 1)
                                    .status(Match.MatchStatus.PENDING)
                                    .build();
                            allMatches.add(match);
                        }
                    }
                    User lastPlayer = rotatingPlayers.remove(numPlayers - 1);
                    rotatingPlayers.add(1, lastPlayer);
                }
            }
        }
        leagueRoom.setStatus(LeagueRoom.RoomStatus.IN_PROGRESS);
        leagueRoomRepository.save(leagueRoom);
        matchRepository.saveAll(allMatches);
    }

    private List<LeagueTeam> ensureTeams(LeagueRoom leagueRoom, List<User> participants) {
        List<LeagueTeam> existingTeams = leagueTeamRepository.findByLeagueRoom(leagueRoom);
        if (!existingTeams.isEmpty()) {
            return existingTeams;
        }

        int teamSize = leagueRoom.getTeamSize() != null ? leagueRoom.getTeamSize() : 2;
        List<LeagueTeam> createdTeams = new ArrayList<>();
        int teamCount = 0;
        for (int i = 0; i < participants.size(); i += teamSize) {
            List<User> members = participants.subList(i, Math.min(i + teamSize, participants.size()));
            if (members.isEmpty()) continue;
            teamCount++;
            String teamName = "팀 " + teamCount;
            LeagueTeam team = leagueTeamRepository.save(LeagueTeam.builder()
                    .leagueRoom(leagueRoom)
                    .name(teamName)
                    .build());
            for (User member : members) {
                LeagueTeamMember teamMember = LeagueTeamMember.builder()
                        .team(team)
                        .user(member)
                        .build();
                leagueTeamMemberRepository.save(teamMember);
            }
            createdTeams.add(team);
        }
        return createdTeams;
    }

    @Transactional(readOnly = true)
    public List<Match> getMatchesByLeagueRoom(LeagueRoom leagueRoom) {
        return matchRepository.findByLeagueRoomWithDetails(leagueRoom);
    }

    public Match updateMatchResult(Long matchId, int player1Score, int player2Score) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 경기입니다."));
        if (match.getStatus() == Match.MatchStatus.COMPLETED) {
            throw new IllegalStateException("이미 종료된 경기 결과는 수정할 수 없습니다.");
        }
        match.setPlayer1Score(player1Score);
        match.setPlayer2Score(player2Score);
        if (match.getTeam1() != null || match.getTeam2() != null) {
            LeagueTeam winnerTeam = (player1Score > player2Score) ? match.getTeam1() : match.getTeam2();
            match.setWinnerTeam(winnerTeam);
            match.setWinner(null);
        } else {
            User winner = (player1Score > player2Score) ? match.getPlayer1() : match.getPlayer2();
            match.setWinner(winner);
            match.setWinnerTeam(null);
        }
        match.setStatus(Match.MatchStatus.COMPLETED);
        return matchRepository.save(match);
    }

    public void bulkUpdateMatchResults(List<MatchResultDto> results) {
        for (MatchResultDto result : results) {
            updateMatchResult(result.getMatchId(), result.getPlayer1Score(), result.getPlayer2Score());
        }
    }

    public void bulkUpdateFromGrid(List<GridResultDto> gridResults) {
        for (GridResultDto result : gridResults) {
            Match match = matchRepository.findById(result.getMatchId())
                    .orElseThrow(() -> new IllegalArgumentException("ID " + result.getMatchId() + "에 해당하는 경기를 찾을 수 없습니다."));
            Long matchPlayer1Id = match.getTeam1() != null ? match.getTeam1().getId() : match.getPlayer1().getId();
            if (matchPlayer1Id.equals(result.getRowPlayerId())) {
                updateMatchResult(result.getMatchId(), result.getRowPlayerScore(), result.getColPlayerScore());
            } else {
                updateMatchResult(result.getMatchId(), result.getColPlayerScore(), result.getRowPlayerScore());
            }
        }
    }

    /**
     * 컨트롤러에서 호출하는 메인 메소드
     * @param roomId 리그 방 ID
     */
    public void advanceToFinals(Long roomId) {
        LeagueRoom leagueRoom = leagueRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("리그 방을 찾을 수 없습니다."));

        if (leagueRoom.getGameType() == LeagueRoom.GameType.TEAM) {
            Map<String, List<LeagueTeam>> finalistsByGroup = selectFinalTeamsByGroup(leagueRoom);
            generateTournamentBracketForTeams(leagueRoom, finalistsByGroup);
        } else {
            // 1. 진출자 선발 (조별로 그룹화)
            Map<String, List<User>> finalistsByGroup = selectFinalistsByGroup(leagueRoom);

            // 2. 대진표 생성
            generateTournamentBracket(leagueRoom, finalistsByGroup);
        }
    }

    /**
     * 1단계: 예선전 결과를 바탕으로 본선 진출자를 선발하는 로직
     * @param leagueRoom 리그 방
     * @return 선발된 최종 진출자 리스트 (조별로 그룹화된 맵)
     */
    @Transactional(readOnly = true)
    public Map<String, List<User>> selectFinalistsByGroup(LeagueRoom leagueRoom) {
        log.info("본선 진출자 선발을 시작합니다. Room ID: {}", leagueRoom.getId());

        List<Match> allMatches = matchRepository.findByLeagueRoomWithDetails(leagueRoom);
        Map<String, List<Match>> groupedMatches = allMatches.stream()
                .filter(match -> match.getLeagueGroup() != null)
                .collect(Collectors.groupingBy(match -> match.getLeagueGroup().getGroupName()));

        if (groupedMatches.isEmpty()) {
            throw new IllegalStateException("순위를 계산할 예선 경기가 없습니다.");
        }

        Map<String, List<User>> finalistsByGroup = new LinkedHashMap<>();
        int advancingPlayersCount = leagueRoom.getAdvancingPlayersPerGroup();
        log.info("설정된 조별 본선 진출 인원: {}", advancingPlayersCount);

        List<String> sortedGroupNames = new ArrayList<>(groupedMatches.keySet());
        Collections.sort(sortedGroupNames);

        LeagueRoom.RoundRobinRankingType rankingType = leagueRoom.getRoundRobinRankingType() != null
                ? leagueRoom.getRoundRobinRankingType()
                : LeagueRoom.RoundRobinRankingType.POINTS;

        for (String groupName : sortedGroupNames) {
            List<Match> groupMatches = groupedMatches.get(groupName);
            List<GroupStandingDto> standings = rankingService.calculateGroupStandings(groupMatches, rankingType);

            log.info("{} 순위표 크기: {}", groupName, standings.size());

            List<User> groupFinalists = new ArrayList<>();
            for (int i = 0; i < advancingPlayersCount && i < standings.size(); i++) {
                long playerId = standings.get(i).getPlayerId();
                userRepository.findById(playerId).ifPresent(groupFinalists::add);
            }
            finalistsByGroup.put(groupName, groupFinalists);
        }
        
        int totalFinalists = finalistsByGroup.values().stream().mapToInt(List::size).sum();
        log.info("최종 선발된 본선 진출자 수: {}", totalFinalists);
        return finalistsByGroup;
    }

    @Transactional(readOnly = true)
    public Map<String, List<LeagueTeam>> selectFinalTeamsByGroup(LeagueRoom leagueRoom) {
        log.info("본선 진출 팀 선발을 시작합니다. Room ID: {}", leagueRoom.getId());

        List<Match> allMatches = matchRepository.findByLeagueRoomWithDetails(leagueRoom);
        Map<String, List<Match>> groupedMatches = allMatches.stream()
                .filter(match -> match.getLeagueGroup() != null)
                .collect(Collectors.groupingBy(match -> match.getLeagueGroup().getGroupName()));

        if (groupedMatches.isEmpty()) {
            throw new IllegalStateException("순위를 계산할 예선 경기가 없습니다.");
        }

        Map<String, List<LeagueTeam>> finalistsByGroup = new LinkedHashMap<>();
        int advancingTeamsCount = leagueRoom.getAdvancingPlayersPerGroup();
        LeagueRoom.RoundRobinRankingType rankingType = leagueRoom.getRoundRobinRankingType() != null
                ? leagueRoom.getRoundRobinRankingType()
                : LeagueRoom.RoundRobinRankingType.POINTS;

        List<String> sortedGroupNames = new ArrayList<>(groupedMatches.keySet());
        Collections.sort(sortedGroupNames);

        for (String groupName : sortedGroupNames) {
            List<Match> groupMatches = groupedMatches.get(groupName);
            List<GroupStandingDto> standings = rankingService.calculateGroupStandings(groupMatches, rankingType);

            List<LeagueTeam> groupFinalists = new ArrayList<>();
            for (int i = 0; i < advancingTeamsCount && i < standings.size(); i++) {
                long teamId = standings.get(i).getPlayerId();
                leagueTeamRepository.findById(teamId).ifPresent(groupFinalists::add);
            }
            finalistsByGroup.put(groupName, groupFinalists);
        }

        return finalistsByGroup;
    }

    /**
     * 하위 호환성을 위한 기존 메서드
     */
    @Transactional(readOnly = true)
    public List<User> selectFinalists(LeagueRoom leagueRoom) {
        Map<String, List<User>> finalistsByGroup = selectFinalistsByGroup(leagueRoom);
        return finalistsByGroup.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * 2단계: 선발된 진출자들로 토너먼트 대진표를 생성하는 로직
     * 전체 진출자를 좌우 브래킷으로 나눠서 결승전에서 만나도록 함
     * @param leagueRoom 리그 방
     * @param finalistsByGroup 조별로 그룹화된 진출자 맵
     */
    public void generateTournamentBracket(LeagueRoom leagueRoom, Map<String, List<User>> finalistsByGroup) {
        log.info("본선 토너먼트 대진표 생성을 시작합니다. 조 개수: {}", finalistsByGroup.size());

        // 1. 조별 순위별로 선수를 정리
        List<User> allFinalists = new ArrayList<>();
        
        // 조 이름 순으로 정렬하여 일관성 유지
        List<String> groupNames = new ArrayList<>(finalistsByGroup.keySet());
        Collections.sort(groupNames);
        
        // 각 조에서 순위별로 선수 수집
        int maxPlayersPerGroup = finalistsByGroup.values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);
        
        // 순위별로 선수를 추가 (1위들 먼저, 그 다음 2위들...)
        for (int rank = 0; rank < maxPlayersPerGroup; rank++) {
            for (String groupName : groupNames) {
                List<User> groupPlayers = finalistsByGroup.get(groupName);
                if (rank < groupPlayers.size()) {
                    allFinalists.add(groupPlayers.get(rank));
                }
            }
        }

        if (allFinalists.size() < 2) {
            throw new IllegalStateException("본선에 진출할 선수가 부족합니다.");
        }

        int totalPlayers = allFinalists.size();
        int bracketSize = 1;
        while (bracketSize < totalPlayers) {
            bracketSize *= 2;
        }
        log.info("{}명 진출, 브라켓 사이즈: {}", totalPlayers, bracketSize);

        // 2. 좌우 브래킷으로 균등 분할
        int halfSize = bracketSize / 2;
        List<User> leftBracket = new ArrayList<>();
        List<User> rightBracket = new ArrayList<>();
        
        // 전체 선수의 앞쪽 절반을 왼쪽에, 뒤쪽 절반을 오른쪽에 배치
        for (int i = 0; i < allFinalists.size(); i++) {
            if (i < (allFinalists.size() + 1) / 2) {
                leftBracket.add(allFinalists.get(i));
            } else {
                rightBracket.add(allFinalists.get(i));
            }
        }
        
        // BYE 추가 (좌우 균등하게)
        int totalByes = bracketSize - totalPlayers;
        int leftByes = totalByes / 2;
        int rightByes = totalByes - leftByes;
        
        for (int i = 0; i < leftByes; i++) {
            leftBracket.add(null);
        }
        for (int i = 0; i < rightByes; i++) {
            rightBracket.add(null);
        }

        // 3. 최종 브래킷 배치 (왼쪽 + 오른쪽)
        List<User> bracketEntrants = new ArrayList<>();
        bracketEntrants.addAll(leftBracket);
        bracketEntrants.addAll(rightBracket);

        log.info("좌우 대진표 배치 완료: 왼쪽 {}명, 오른쪽 {}명", leftBracket.size(), rightBracket.size());
        log.info("왼쪽 선수들: {}", leftBracket.stream()
                .map(u -> u != null ? u.getNickname() : "BYE")
                .collect(Collectors.joining(", ")));
        log.info("오른쪽 선수들: {}", rightBracket.stream()
                .map(u -> u != null ? u.getNickname() : "BYE")
                .collect(Collectors.joining(", ")));

        // 4. 1라운드 매치 생성
        List<Match> finalMatches = new ArrayList<>();
        int roundNumber = 1;

        for (int i = 0; i < bracketSize / 2; i++) {
            User player1 = bracketEntrants.get(i);
            User player2 = bracketEntrants.get(bracketSize - 1 - i);

            Match match;
            if (player1 == null || player2 == null) {
                User byeWinner = (player1 != null) ? player1 : player2;
                match = createFinalsMatch(leagueRoom, byeWinner, null, roundNumber);
                match.setWinner(byeWinner);
                match.setStatus(Match.MatchStatus.COMPLETED);
                match.setPlayer1Score(1);
                match.setPlayer2Score(0);
            } else {
                match = createFinalsMatch(leagueRoom, player1, player2, roundNumber);
            }
            finalMatches.add(match);
        }

        matchRepository.saveAll(finalMatches);
        log.info("본선 1라운드 {}개 경기 생성 완료.", finalMatches.size());
    }

    public void generateTournamentBracketForTeams(LeagueRoom leagueRoom, Map<String, List<LeagueTeam>> finalistsByGroup) {
        log.info("본선 토너먼트 대진표(팀) 생성을 시작합니다. 조 개수: {}", finalistsByGroup.size());

        List<LeagueTeam> allFinalists = new ArrayList<>();
        List<String> groupNames = new ArrayList<>(finalistsByGroup.keySet());
        Collections.sort(groupNames);

        int maxTeamsPerGroup = finalistsByGroup.values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);

        for (int rank = 0; rank < maxTeamsPerGroup; rank++) {
            for (String groupName : groupNames) {
                List<LeagueTeam> groupTeams = finalistsByGroup.get(groupName);
                if (rank < groupTeams.size()) {
                    allFinalists.add(groupTeams.get(rank));
                }
            }
        }

        if (allFinalists.size() < 2) {
            throw new IllegalStateException("본선에 진출할 팀이 부족합니다.");
        }

        int totalTeams = allFinalists.size();
        int bracketSize = 1;
        while (bracketSize < totalTeams) {
            bracketSize *= 2;
        }

        int halfSize = bracketSize / 2;
        List<LeagueTeam> leftBracket = new ArrayList<>();
        List<LeagueTeam> rightBracket = new ArrayList<>();

        for (int i = 0; i < allFinalists.size(); i++) {
            if (i < (allFinalists.size() + 1) / 2) {
                leftBracket.add(allFinalists.get(i));
            } else {
                rightBracket.add(allFinalists.get(i));
            }
        }

        int totalByes = bracketSize - totalTeams;
        int leftByes = totalByes / 2;
        int rightByes = totalByes - leftByes;

        for (int i = 0; i < leftByes; i++) {
            leftBracket.add(null);
        }
        for (int i = 0; i < rightByes; i++) {
            rightBracket.add(null);
        }

        List<LeagueTeam> bracketEntrants = new ArrayList<>();
        bracketEntrants.addAll(leftBracket);
        bracketEntrants.addAll(rightBracket);

        List<Match> finalMatches = new ArrayList<>();
        int roundNumber = 1;

        for (int i = 0; i < bracketSize / 2; i++) {
            LeagueTeam team1 = bracketEntrants.get(i);
            LeagueTeam team2 = bracketEntrants.get(bracketSize - 1 - i);

            Match match;
            if (team1 == null || team2 == null) {
                LeagueTeam byeWinner = (team1 != null) ? team1 : team2;
                match = createFinalsMatchForTeams(leagueRoom, byeWinner, null, roundNumber);
                match.setWinnerTeam(byeWinner);
                match.setStatus(Match.MatchStatus.COMPLETED);
                match.setPlayer1Score(1);
                match.setPlayer2Score(0);
            } else {
                match = createFinalsMatchForTeams(leagueRoom, team1, team2, roundNumber);
            }
            finalMatches.add(match);
        }

        matchRepository.saveAll(finalMatches);
        log.info("본선 1라운드 팀 경기 {}개 생성 완료.", finalMatches.size());
    }

    private Match createFinalsMatch(LeagueRoom room, User p1, User p2, int round) {
        return Match.builder()
                .leagueRoom(room)
                .leagueGroup(null) // 본선은 그룹 없음
                .player1(p1)
                .player2(p2)
                .team1(null)
                .team2(null)
                .roundNumber(round)
                .status(Match.MatchStatus.PENDING)
                .build();
    }

    private Match createFinalsMatchForTeams(LeagueRoom room, LeagueTeam t1, LeagueTeam t2, int round) {
        return Match.builder()
                .leagueRoom(room)
                .leagueGroup(null)
                .player1(null)
                .player2(null)
                .team1(t1)
                .team2(t2)
                .roundNumber(round)
                .status(Match.MatchStatus.PENDING)
                .build();
    }

    public void generateNextRound(Long roomId) {
        LeagueRoom leagueRoom = leagueRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("리그 방을 찾을 수 없습니다."));

        int lastRound = matchRepository.findLatestTournamentRoundNumber(leagueRoom)
                .orElseThrow(() -> new IllegalStateException("진행된 토너먼트 라운드가 없습니다."));

        boolean isRoundFinished = !matchRepository.existsByLeagueRoomAndRoundNumberAndStatus(
                leagueRoom, lastRound, Match.MatchStatus.PENDING);

        if (!isRoundFinished) {
            throw new IllegalStateException("아직 " + lastRound + "라운드의 모든 경기가 종료되지 않았습니다.");
        }

        if (leagueRoom.getGameType() == LeagueRoom.GameType.TEAM) {
            List<LeagueTeam> winners = matchRepository.findTournamentMatchesByRound(leagueRoom, lastRound)
                    .stream()
                    .map(Match::getWinnerTeam)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (winners.size() == 1) {
                leagueRoom.setStatus(LeagueRoom.RoomStatus.COMPLETED);
                leagueRoomRepository.save(leagueRoom);
                return;
            }

            List<Match> nextRoundMatches = new ArrayList<>();
            int nextRoundNumber = lastRound + 1;

            for (int i = 0; i < winners.size(); i += 2) {
                LeagueTeam team1 = winners.get(i);
                if (i + 1 >= winners.size()) {
                    Match byeMatch = createFinalsMatchForTeams(leagueRoom, team1, null, nextRoundNumber);
                    byeMatch.setWinnerTeam(team1);
                    byeMatch.setStatus(Match.MatchStatus.COMPLETED);
                    byeMatch.setPlayer1Score(1);
                    byeMatch.setPlayer2Score(0);
                    nextRoundMatches.add(byeMatch);
                } else {
                    LeagueTeam team2 = winners.get(i + 1);
                    Match match = createFinalsMatchForTeams(leagueRoom, team1, team2, nextRoundNumber);
                    nextRoundMatches.add(match);
                }
            }
            matchRepository.saveAll(nextRoundMatches);
        } else {
            List<User> winners = matchRepository.findTournamentMatchesByRound(leagueRoom, lastRound)
                    .stream()
                    .map(Match::getWinner)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            // ================== 디버깅 코드 추가 ==================
            System.out.println("### 디버깅: 2라운드 승리자 수 = " + winners.size());
            // ======================================================

            if (winners.size() == 1) {
                leagueRoom.setStatus(LeagueRoom.RoomStatus.COMPLETED);
                leagueRoomRepository.save(leagueRoom);
                return;
            }

            // Collections.shuffle(winners);
            List<Match> nextRoundMatches = new ArrayList<>();
            int nextRoundNumber = lastRound + 1;

            for (int i = 0; i < winners.size(); i += 2) {
                User player1 = winners.get(i);
                if (i + 1 >= winners.size()) {
                    Match byeMatch = createFinalsMatch(leagueRoom, player1, null, nextRoundNumber);
                    byeMatch.setWinner(player1);
                    byeMatch.setStatus(Match.MatchStatus.COMPLETED);
                    byeMatch.setPlayer1Score(1);
                    byeMatch.setPlayer2Score(0);
                    nextRoundMatches.add(byeMatch);
                } else {
                    User player2 = winners.get(i + 1);
                    Match match = createFinalsMatch(leagueRoom, player1, player2, nextRoundNumber);
                    nextRoundMatches.add(match);
                }
            }
            matchRepository.saveAll(nextRoundMatches);
        }
    }

    public void autoCompleteGroupStage(Long roomId) {
        LeagueRoom leagueRoom = leagueRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("리그 방을 찾을 수 없습니다."));

        List<Match> pendingGroupMatches = matchRepository.findByLeagueRoomWithDetails(leagueRoom).stream()
                .filter(match -> match.getLeagueGroup() != null && match.getStatus() == Match.MatchStatus.PENDING)
                .toList();

        if (pendingGroupMatches.isEmpty()) {
            throw new IllegalStateException("자동으로 완료할 예선 경기가 없습니다.");
        }

        Random random = new Random();

        for (Match match : pendingGroupMatches) {
            int winnerScore = 3;
            int loserScore = random.nextInt(3);
            boolean player1Wins = random.nextBoolean();
            int player1Score = player1Wins ? winnerScore : loserScore;
            int player2Score = player1Wins ? loserScore : winnerScore;
            updateMatchResult(match.getId(), player1Score, player2Score);
        }
    }

    public void autoCompleteTournamentRound(Long roomId) {
        LeagueRoom leagueRoom = leagueRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("리그 방을 찾을 수 없습니다."));

        int latestRound = matchRepository.findLatestTournamentRoundNumber(leagueRoom)
                .orElseThrow(() -> new IllegalStateException("진행된 토너먼트 라운드가 없습니다."));

        List<Match> pendingMatches = matchRepository.findByLeagueRoomAndRoundNumberAndStatus(
                leagueRoom, latestRound, Match.MatchStatus.PENDING);

        if (pendingMatches.isEmpty()) {
            throw new IllegalStateException("자동으로 완료할 경기가 없습니다.");
        }

        Random random = new Random();
        for (Match match : pendingMatches) {
            int winnerScore = 3;
            int loserScore = random.nextInt(3);
            boolean player1Wins = random.nextBoolean();
            int player1Score = player1Wins ? winnerScore : loserScore;
            int player2Score = player1Wins ? loserScore : winnerScore;
            updateMatchResult(match.getId(), player1Score, player2Score);
        }
    }

    /**
     * 최종 순위 (1, 2, 공동 3위)를 계산하여 반환합니다. (수정된 최종 버전)
     * @param leagueRoom 리그 방
     * @return FinalResultDto
     */
    @Transactional(readOnly = true)
    public FinalResultDto getFinalResults(LeagueRoom leagueRoom) {

        if (leagueRoom.getMatchFormat() == LeagueRoom.MatchFormat.PRELIMINARY_TOURNAMENT) {
            // 1. 예선+본선 토너먼트 방식의 결과 계산 (기존 로직 유지)
            List<Match> tournamentMatches = matchRepository.findByLeagueRoomWithDetails(leagueRoom).stream()
                    .filter(m -> m.getLeagueGroup() == null)
                    .toList();

            if (tournamentMatches.isEmpty()) {
                throw new IllegalStateException("결과를 집계할 본선 토너먼트 경기가 없습니다.");
            }

            int finalRoundNum = tournamentMatches.stream().mapToInt(Match::getRoundNumber).max().orElse(0);
            if (finalRoundNum < 2) {
                throw new IllegalStateException("결승 또는 준결승 경기가 없어 순위를 집계할 수 없습니다.");
            }
            int semiFinalRoundNum = finalRoundNum - 1;

            Match finalMatch = tournamentMatches.stream()
                    .filter(m -> m.getRoundNumber() == finalRoundNum)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("결승전 경기를 찾을 수 없습니다."));

            List<Match> semiFinalMatches = tournamentMatches.stream()
                    .filter(m -> m.getRoundNumber() == semiFinalRoundNum)
                    .toList();

            if (leagueRoom.getGameType() == LeagueRoom.GameType.TEAM) {
                if (finalMatch.getWinnerTeam() == null || semiFinalMatches.size() < 2) {
                    throw new IllegalStateException("순위를 집계하기에 경기 데이터가 부족합니다. (결승전 또는 준결승전 결과 누락)");
                }

                LeagueTeam winner = finalMatch.getWinnerTeam();
                LeagueTeam runnerUp = finalMatch.getTeam1().getId().equals(winner.getId()) ? finalMatch.getTeam2() : finalMatch.getTeam1();

                List<String> jointThird = semiFinalMatches.stream()
                        .map(match -> match.getTeam1().getId().equals(match.getWinnerTeam().getId()) ? match.getTeam2() : match.getTeam1())
                        .map(LeagueTeam::getName)
                        .toList();

                return FinalResultDto.builder()
                        .winner(winner.getName())
                        .runnerUp(runnerUp.getName())
                        .jointThird(jointThird)
                        .rankings(null)
                        .build();
            } else {
                if (finalMatch.getWinner() == null || semiFinalMatches.size() < 2) {
                    throw new IllegalStateException("순위를 집계하기에 경기 데이터가 부족합니다. (결승전 또는 준결승전 결과 누락)");
                }

                User winner = finalMatch.getWinner();
                User runnerUp = finalMatch.getPlayer1().getId().equals(winner.getId()) ? finalMatch.getPlayer2() : finalMatch.getPlayer1();

                List<String> jointThird = semiFinalMatches.stream()
                        .map(match -> match.getPlayer1().getId().equals(match.getWinner().getId()) ? match.getPlayer2() : match.getPlayer1())
                        .map(User::getNickname)
                        .toList();

                return FinalResultDto.builder()
                        .winner(winner.getNickname())
                        .runnerUp(runnerUp.getNickname())
                        .jointThird(jointThird)
                        .rankings(null)
                        .build();
            }

        } else {
            // ▼▼▼ 2. 조별 풀리그 방식의 결과 계산 (새로운 로직) ▼▼▼
            List<Match> allMatches = matchRepository.findByLeagueRoomWithDetails(leagueRoom);
            if (allMatches.stream().anyMatch(m -> m.getStatus() == Match.MatchStatus.PENDING)) {
                throw new IllegalStateException("아직 모든 경기가 종료되지 않아 최종 결과를 집계할 수 없습니다.");
            }

            List<Match> groupMatches = allMatches.stream()
                    .filter(m -> m.getLeagueGroup() != null)
                    .toList();

            LeagueRoom.RoundRobinRankingType rankingType = leagueRoom.getRoundRobinRankingType() != null
                    ? leagueRoom.getRoundRobinRankingType()
                    : LeagueRoom.RoundRobinRankingType.POINTS;

            List<GroupStandingDto> standings = rankingService.calculateGroupStandings(groupMatches, rankingType);
            List<String> fullRankings = standings.stream()
                    .map(s -> String.format("%d위 %s (승 %d / 패 %d / 승점 %d)", s.getRank(), s.getPlayerName(), s.getWins(), s.getLosses(), s.getPoints()))
                    .toList();

            String winner = standings.size() >= 1 ? standings.get(0).getPlayerName() : "";
            String runnerUp = standings.size() >= 2 ? standings.get(1).getPlayerName() : "";
            List<String> jointThird = standings.size() >= 3 ? List.of(standings.get(2).getPlayerName()) : List.of();

            return FinalResultDto.builder()
                    .winner(winner)
                    .runnerUp(runnerUp)
                    .jointThird(jointThird)
                    .rankings(fullRankings)
                    .build();
        }
    }

    /**
     * 토너먼트 대진표 데이터를 생성하여 반환
     */
    @Transactional(readOnly = true)
    public TournamentDataDto getTournamentData(LeagueRoom leagueRoom) {
        List<Match> allMatches = matchRepository.findByLeagueRoomWithDetails(leagueRoom);
        List<Match> mainTournamentMatches = allMatches.stream()
                .filter(m -> m.getLeagueGroup() == null)
                .collect(Collectors.toList());

        if (mainTournamentMatches.isEmpty()) {
            return new TournamentDataDto(null, null);
        }

        Map<Integer, List<Match>> rounds = mainTournamentMatches.stream()
                .collect(Collectors.groupingBy(Match::getRoundNumber, TreeMap::new, Collectors.toList()));

        Map<Long, String> groupRankLabels = buildGroupRankLabels(leagueRoom, allMatches);

        Map<String, Object> standardData = buildStandardTournamentData(rounds);
        Map<String, Object> splitData = buildSplitTournamentData(rounds, groupRankLabels);

        return new TournamentDataDto(standardData, splitData);
    }

    private Map<Long, String> buildGroupRankLabels(LeagueRoom leagueRoom, List<Match> allMatches) {
        Map<String, List<Match>> groupedMatches = allMatches.stream()
                .filter(match -> match.getLeagueGroup() != null)
                .collect(Collectors.groupingBy(match -> match.getLeagueGroup().getGroupName()));

        if (groupedMatches.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, String> labels = new HashMap<>();
        List<String> sortedGroupNames = new ArrayList<>(groupedMatches.keySet());
        Collections.sort(sortedGroupNames);

        LeagueRoom.RoundRobinRankingType rankingType = leagueRoom.getRoundRobinRankingType() != null
                ? leagueRoom.getRoundRobinRankingType()
                : LeagueRoom.RoundRobinRankingType.POINTS;

        for (String groupName : sortedGroupNames) {
            List<GroupStandingDto> standings = rankingService.calculateGroupStandings(groupedMatches.get(groupName), rankingType);
            for (GroupStandingDto standing : standings) {
                if (standing.getRank() > 0) {
                    labels.put(standing.getPlayerId(), String.format("%s %d위", groupName, standing.getRank()));
                }
            }
        }

        return labels;
    }

    /**
     * 단방향 (순차 진행) 토너먼트 데이터 생성
     * 모든 선수가 순서대로 배치됨
     */
    private Map<String, Object> buildStandardTournamentData(Map<Integer, List<Match>> rounds) {
        Map<String, Object> tournamentData = new HashMap<>();
        
        List<Match> firstRoundMatches = rounds.get(rounds.keySet().stream().min(Integer::compareTo).orElse(1));
        firstRoundMatches.sort(Comparator.comparing(Match::getId));
        
        List<List<String>> teams = firstRoundMatches.stream()
                .map(match -> Arrays.asList(
                        getCompetitorName(match, true),
                        getCompetitorName(match, false)
                ))
                .collect(Collectors.toList());
        tournamentData.put("teams", teams);

        List<List<List<Object>>> results = rounds.values().stream()
                .map(roundMatches -> {
                    roundMatches.sort(Comparator.comparing(Match::getId));
                    return roundMatches.stream()
                            .map(m -> {
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("matchId", m.getId());
                                userData.put("player1Name", getCompetitorName(m, true));
                                userData.put("player2Name", getCompetitorName(m, false));
                                return Arrays.asList(m.getPlayer1Score(), m.getPlayer2Score(), userData);
                            })
                            .collect(Collectors.toList());
                })
                .collect(Collectors.toList());
        tournamentData.put("results", results);

        return tournamentData;
    }

    /**
     * 양방향 (좌우 대진) 토너먼트 데이터 생성
     * 1라운드를 절반으로 나눠서 왼쪽 브라켓과 오른쪽 브라켓으로 구성
     */
    private Map<String, Object> buildSplitTournamentData(Map<Integer, List<Match>> rounds, Map<Long, String> groupRankLabels) {
        Map<String, Object> tournamentData = new HashMap<>();
        
        List<Match> firstRoundMatches = rounds.get(rounds.keySet().stream().min(Integer::compareTo).orElse(1));
        firstRoundMatches.sort(Comparator.comparing(Match::getId));
        
        int totalFirstRound = firstRoundMatches.size();
        int halfSize = totalFirstRound / 2;
        
        // 왼쪽 브라켓: 1라운드 전반부
        List<Match> leftMatches = firstRoundMatches.subList(0, halfSize);
        // 오른쪽 브라켓: 1라운드 후반부 (역순으로)
        List<Match> rightMatches = new ArrayList<>(firstRoundMatches.subList(halfSize, totalFirstRound));
        Collections.reverse(rightMatches);
        
        // teams 배열: [왼쪽 브라켓 매치들] + [오른쪽 브라켓 매치들]
        List<List<String>> teams = new ArrayList<>();
        for (Match match : leftMatches) {
            teams.add(Arrays.asList(
                    getCompetitorName(match, true),
                    getCompetitorName(match, false)
            ));
        }
        for (Match match : rightMatches) {
            teams.add(Arrays.asList(
                    getCompetitorName(match, true),
                    getCompetitorName(match, false)
            ));
        }
        tournamentData.put("teams", teams);

        // results 배열: 각 라운드별 매치 결과
        List<List<List<Object>>> results = new ArrayList<>();
        
        // 1라운드: 왼쪽 + 오른쪽 순서로
        List<List<Object>> round1Results = new ArrayList<>();
        for (Match m : leftMatches) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("matchId", m.getId());
            userData.put("player1Name", getCompetitorName(m, true));
            userData.put("player2Name", getCompetitorName(m, false));
            userData.put("player1GroupRank", getGroupRankLabel(getCompetitorId(m, true), groupRankLabels));
            userData.put("player2GroupRank", getGroupRankLabel(getCompetitorId(m, false), groupRankLabels));
            round1Results.add(Arrays.asList(m.getPlayer1Score(), m.getPlayer2Score(), userData));
        }
        for (Match m : rightMatches) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("matchId", m.getId());
            userData.put("player1Name", getCompetitorName(m, true));
            userData.put("player2Name", getCompetitorName(m, false));
            userData.put("player1GroupRank", getGroupRankLabel(getCompetitorId(m, true), groupRankLabels));
            userData.put("player2GroupRank", getGroupRankLabel(getCompetitorId(m, false), groupRankLabels));
            round1Results.add(Arrays.asList(m.getPlayer1Score(), m.getPlayer2Score(), userData));
        }
        results.add(round1Results);
        
        // 2라운드부터는 자동으로 양쪽에서 수렴
        List<Integer> roundNumbers = new ArrayList<>(rounds.keySet());
        Collections.sort(roundNumbers);
        for (int i = 1; i < roundNumbers.size(); i++) {
            int roundNum = roundNumbers.get(i);
            List<Match> roundMatches = new ArrayList<>(rounds.get(roundNum));
            roundMatches.sort(Comparator.comparing(Match::getId));
            
            // 2라운드 이상도 왼쪽/오른쪽 절반으로 나눔
            int roundHalfSize = roundMatches.size() / 2;
            List<Match> leftRound = roundMatches.subList(0, Math.max(1, roundHalfSize));
            List<Match> rightRound = roundMatches.size() > 1 
                ? new ArrayList<>(roundMatches.subList(roundHalfSize, roundMatches.size())) 
                : new ArrayList<>();
            Collections.reverse(rightRound);
            
            List<List<Object>> roundResults = new ArrayList<>();
            for (Match m : leftRound) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("matchId", m.getId());
                userData.put("player1Name", getCompetitorName(m, true));
                userData.put("player2Name", getCompetitorName(m, false));
                userData.put("player1GroupRank", getGroupRankLabel(getCompetitorId(m, true), groupRankLabels));
                userData.put("player2GroupRank", getGroupRankLabel(getCompetitorId(m, false), groupRankLabels));
                roundResults.add(Arrays.asList(m.getPlayer1Score(), m.getPlayer2Score(), userData));
            }
            for (Match m : rightRound) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("matchId", m.getId());
                userData.put("player1Name", getCompetitorName(m, true));
                userData.put("player2Name", getCompetitorName(m, false));
                userData.put("player1GroupRank", getGroupRankLabel(getCompetitorId(m, true), groupRankLabels));
                userData.put("player2GroupRank", getGroupRankLabel(getCompetitorId(m, false), groupRankLabels));
                roundResults.add(Arrays.asList(m.getPlayer1Score(), m.getPlayer2Score(), userData));
            }
            results.add(roundResults);
        }
        
        tournamentData.put("results", results);
        return tournamentData;
    }

    private String getGroupRankLabel(Long competitorId, Map<Long, String> groupRankLabels) {
        if (competitorId == null || groupRankLabels == null || groupRankLabels.isEmpty()) {
            return null;
        }
        return groupRankLabels.get(competitorId);
    }

    private String getCompetitorName(Match match, boolean first) {
        if (match.getTeam1() != null || match.getTeam2() != null) {
            LeagueTeam team = first ? match.getTeam1() : match.getTeam2();
            return team != null ? team.getName() : "BYE";
        }
        User player = first ? match.getPlayer1() : match.getPlayer2();
        return player != null ? player.getNickname() : "BYE";
    }

    private Long getCompetitorId(Match match, boolean first) {
        if (match.getTeam1() != null || match.getTeam2() != null) {
            LeagueTeam team = first ? match.getTeam1() : match.getTeam2();
            return team != null ? team.getId() : null;
        }
        User player = first ? match.getPlayer1() : match.getPlayer2();
        return player != null ? player.getId() : null;
    }

}
