package com.maniasin.pingpongleague.controller;

import com.maniasin.pingpongleague.domain.LeagueRoom;
import com.maniasin.pingpongleague.domain.Match;
import com.maniasin.pingpongleague.domain.User;
import com.maniasin.pingpongleague.dto.FinalResultDto;
import com.maniasin.pingpongleague.dto.GroupDetailDto;
import com.maniasin.pingpongleague.dto.GroupStandingDto;
import com.maniasin.pingpongleague.dto.LeagueRoomResponseDto;
import com.maniasin.pingpongleague.dto.MatchDetailDto;
import com.maniasin.pingpongleague.repository.UserRepository;
import com.maniasin.pingpongleague.service.LeagueRoomService;
import com.maniasin.pingpongleague.service.MatchService;
import com.maniasin.pingpongleague.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final LeagueRoomService leagueRoomService;
    private final MatchService matchService;
    private final RankingService rankingService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper; // ObjectMapper 필드 추가

    // ... (loginPage, signupPage 등 다른 GET 매핑 메소드는 그대로 유지) ...

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @GetMapping("/signup")
    public String signupPage() { return "signup"; }

    @GetMapping("/")
    public String homePage() { return "index"; }

    @GetMapping("/league-rooms/new")
    public String createLeagueRoomPage(Model model) {
        model.addAttribute("gameTypes", LeagueRoom.GameType.values());
        return "create-league-room";
    }

    @GetMapping("/league-rooms/{roomId}")
    public String leagueRoomDetailPage(@PathVariable Long roomId, Model model) {
        LeagueRoomResponseDto leagueRoom = new LeagueRoomResponseDto(leagueRoomService.getLeagueRoomById(roomId));
        model.addAttribute("leagueRoom", leagueRoom);
        return "league-room-detail";
    }

    @GetMapping("/league-rooms/{roomId}/edit")
    public String editLeagueRoomPage(@PathVariable Long roomId, Model model) {
        LeagueRoom leagueRoom = leagueRoomService.getLeagueRoomById(roomId);
        model.addAttribute("leagueRoom", new LeagueRoomResponseDto(leagueRoom));
        return "edit-league-room";
    }

    @GetMapping("/league-rooms/{roomId}/matches")
    public String matchListPage(@PathVariable Long roomId, Model model) throws JsonProcessingException {
        LeagueRoom leagueRoom = leagueRoomService.getLeagueRoomById(roomId);
        List<Match> allMatches = matchService.getMatchesByLeagueRoom(leagueRoom);
        boolean isLeagueCompleted = leagueRoom.getStatus() == LeagueRoom.RoomStatus.COMPLETED;

        // --- 예선전 데이터 처리 ---
        List<GroupDetailDto> groupDetails = new ArrayList<>();
        Map<String, List<Match>> originalGroupedMatches = allMatches.stream()
                .filter(match -> match.getLeagueGroup() != null && match.getPlayer2() != null)
                .collect(Collectors.groupingBy(match -> match.getLeagueGroup().getGroupName(), TreeMap::new, Collectors.toList()));

        LeagueRoom.RoundRobinRankingType rankingType = leagueRoom.getRoundRobinRankingType() != null
                ? leagueRoom.getRoundRobinRankingType()
                : LeagueRoom.RoundRobinRankingType.POINTS;

        originalGroupedMatches.forEach((groupName, matches) -> {
            List<GroupStandingDto> standings = rankingService.calculateGroupStandings(matches, rankingType);
            boolean allFinished = matches.stream().allMatch(m -> m.getStatus() == Match.MatchStatus.COMPLETED);
            List<com.maniasin.pingpongleague.dto.GridPlayerDto> players = standings.stream()
                    .sorted(Comparator.comparingInt(GroupStandingDto::getRank))
                    .map(s -> new com.maniasin.pingpongleague.dto.GridPlayerDto(s.getPlayerId(), s.getPlayerName()))
                    .collect(Collectors.toList());
            List<MatchDetailDto> matchesDto = matches.stream().map(MatchDetailDto::new).collect(Collectors.toList());
            Map<String, MatchDetailDto> matchMapDto = matches.stream().collect(Collectors.toMap(
                    m -> {
                        Long p1Id = m.getTeam1() != null ? m.getTeam1().getId() : (m.getPlayer1() != null ? m.getPlayer1().getId() : null);
                        Long p2Id = m.getTeam2() != null ? m.getTeam2().getId() : (m.getPlayer2() != null ? m.getPlayer2().getId() : null);
                        if (p1Id == null || p2Id == null) {
                            return String.valueOf(m.getId());
                        }
                        return p1Id < p2Id ? p1Id + "-" + p2Id : p2Id + "-" + p1Id;
                    },
                    MatchDetailDto::new, (m1, m2) -> m1
            ));
            groupDetails.add(new GroupDetailDto(groupName, matchesDto, standings, players, matchMapDto, allFinished));
        });
        model.addAttribute("groupDetails", groupDetails);
        model.addAttribute("leagueRoom", new LeagueRoomResponseDto(leagueRoom));

        // --- 본선 토너먼트 데이터 처리 ---
        List<Match> mainTournamentMatches = allMatches.stream().filter(m -> m.getLeagueGroup() == null).toList();
        boolean allPreliminariesFinished = !originalGroupedMatches.isEmpty() && groupDetails.stream().allMatch(GroupDetailDto::isFinished);
        model.addAttribute("canAdvanceToFinals", leagueRoom.getMatchFormat() == LeagueRoom.MatchFormat.PRELIMINARY_TOURNAMENT &&
                allPreliminariesFinished && mainTournamentMatches.isEmpty());

        if (!mainTournamentMatches.isEmpty()) {
            List<MatchDetailDto> matchDetails = mainTournamentMatches.stream().map(MatchDetailDto::new).collect(Collectors.toList());
            model.addAttribute("tournamentMatchesJson", objectMapper.writeValueAsString(matchDetails));
            model.addAttribute("hasTournamentData", true);
            model.addAttribute("tournamentType", leagueRoom.getTournamentType() != null ? leagueRoom.getTournamentType().name() : "STANDARD");

            model.addAttribute("isLeagueCompleted", isLeagueCompleted);
            Map<Integer, List<Match>> rounds = mainTournamentMatches.stream()
                    .collect(Collectors.groupingBy(Match::getRoundNumber, TreeMap::new, Collectors.toList()));
            int latestRoundNum = rounds.keySet().stream().max(Integer::compareTo).orElse(0);
            if (latestRoundNum > 0) {
                boolean isLatestRoundFinished = rounds.get(latestRoundNum).stream().allMatch(m -> m.getStatus() == Match.MatchStatus.COMPLETED);
                model.addAttribute("isLatestRoundFinished", isLatestRoundFinished);
                model.addAttribute("isFinalMatch", latestRoundNum > 1 && rounds.get(latestRoundNum).size() == 1 && isLatestRoundFinished);
            }
        } else {
            model.addAttribute("hasTournamentData", false);
            model.addAttribute("tournamentMatchesJson", "[]");
        }
        return "match-list";
    }

}
