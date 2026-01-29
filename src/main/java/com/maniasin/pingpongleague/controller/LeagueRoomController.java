package com.maniasin.pingpongleague.controller;

import com.maniasin.pingpongleague.domain.LeagueRoom;
import com.maniasin.pingpongleague.domain.Match;
import com.maniasin.pingpongleague.domain.User;
import com.maniasin.pingpongleague.dto.*;
import com.maniasin.pingpongleague.repository.UserRepository;
import com.maniasin.pingpongleague.service.LeagueRoomService;
import com.maniasin.pingpongleague.service.MatchService;
import com.maniasin.pingpongleague.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/league-rooms")
public class LeagueRoomController {

    private final LeagueRoomService leagueRoomService;
    private final MatchService matchService;
    private final UserRepository userRepository;
    private final RankingService rankingService;

    @PostMapping
    public ResponseEntity<Void> createLeagueRoom(@RequestBody LeagueRoomCreateRequestDto requestDto, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Long roomId = leagueRoomService.createLeagueRoom(
                requestDto.getTitle(),
                currentUser,
                requestDto.getMaxParticipants(),
                requestDto.getGameType(),
                requestDto.getMatchFormat(),
                requestDto.getRoundRobinRankingType(),
                requestDto.getTournamentType(),
                requestDto.getTeamSize(),
                requestDto.getTeamMatchFormat(),
                requestDto.getLocation(),
                requestDto.getVenueAddress(),
                requestDto.getMatchDescription(),
                requestDto.getContactInfo(),
                requestDto.getPlayersPerGroup(),
                requestDto.getAdvancingPlayersPerGroup(), // <--- 이 부분 수정 ---
                requestDto.getEventDate(),
                requestDto.getEventTime()
        );
        return ResponseEntity.created(URI.create("/api/league-rooms/" + roomId)).build();
    }

    @GetMapping
    public ResponseEntity<List<LeagueRoomResponseDto>> getAllLeagueRooms() {
        List<LeagueRoomResponseDto> leagueRooms = leagueRoomService.getAllLeagueRooms().stream()
                .map(LeagueRoomResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(leagueRooms);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<LeagueRoomResponseDto> getLeagueRoomById(@PathVariable Long roomId) {
        LeagueRoom leagueRoom = leagueRoomService.getLeagueRoomById(roomId);
        return ResponseEntity.ok(new LeagueRoomResponseDto(leagueRoom));
    }

    @GetMapping("/{roomId}/participants")
    public ResponseEntity<List<String>> getLeagueRoomParticipants(@PathVariable Long roomId) {
        LeagueRoom leagueRoom = leagueRoomService.getLeagueRoomById(roomId);
        List<String> participantNicknames = leagueRoom.getParticipants().stream()
                .map(participant -> participant.getUser().getNickname())
                .collect(Collectors.toList());
        return ResponseEntity.ok(participantNicknames);
    }

    @GetMapping("/{roomId}/participants-with-id")
    public ResponseEntity<List<ParticipantDto>> getLeagueRoomParticipantsWithId(@PathVariable Long roomId) {
        List<ParticipantDto> participants = leagueRoomService.getLeagueRoomById(roomId)
                .getParticipants().stream()
                .map(p -> new ParticipantDto(p.getUser()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(participants);
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<Void> joinLeagueRoom(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        leagueRoomService.joinLeagueRoom(roomId, currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/participants")
    public ResponseEntity<Void> addParticipant(@PathVariable Long roomId,
                                               @RequestBody ParticipantAddRequestDto requestDto,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        leagueRoomService.addParticipantByIdentifier(roomId, currentUser, requestDto.getIdentifier());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/participants/bulk")
    public ResponseEntity<ParticipantBulkAddResponseDto> addParticipantsBulk(@PathVariable Long roomId,
                                                                             @RequestBody ParticipantBulkAddRequestDto requestDto,
                                                                             @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        ParticipantBulkAddResponseDto result = leagueRoomService.addParticipantsBulk(roomId, currentUser, requestDto.getIdentifiers());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{roomId}/participants/guest")
    public ResponseEntity<Void> addGuestParticipant(@PathVariable Long roomId,
                                                    @RequestBody GuestParticipantRequestDto requestDto,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        leagueRoomService.addGuestParticipant(roomId, currentUser, requestDto.getNickname());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roomId}/teams")
    public ResponseEntity<List<TeamInfoDto>> getTeams(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return ResponseEntity.ok(leagueRoomService.getTeams(roomId, currentUser));
    }

    @PostMapping("/{roomId}/teams")
    public ResponseEntity<List<TeamInfoDto>> saveTeams(@PathVariable Long roomId,
                                                       @RequestBody TeamAssignmentRequestDto requestDto,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return ResponseEntity.ok(leagueRoomService.saveTeams(roomId, currentUser, requestDto));
    }

    @PostMapping("/{roomId}/teams/auto")
    public ResponseEntity<List<TeamInfoDto>> autoAssignTeams(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return ResponseEntity.ok(leagueRoomService.autoAssignTeams(roomId, currentUser));
    }

    @PostMapping("/{roomId}/start")
    public ResponseEntity<Void> startLeague(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        LeagueRoom leagueRoom = leagueRoomService.getLeagueRoomById(roomId);
        if (!leagueRoom.getOwner().getUsername().equals(userDetails.getUsername())) {
            throw new IllegalArgumentException("리그 운영자만 경기를 시작할 수 있습니다.");
        }
        matchService.generateGroupStageMatches(leagueRoom);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteLeagueRoom(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        leagueRoomService.deleteLeagueRoom(roomId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roomId}/reopen")
    public ResponseEntity<Void> reopenLeagueRoom(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        leagueRoomService.reopenLeagueRoom(roomId, currentUser);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<Void> updateLeagueRoom(@PathVariable Long roomId, @RequestBody LeagueRoomUpdateRequestDto requestDto, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        leagueRoomService.updateLeagueRoom(roomId, currentUser, requestDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/complete")
    public ResponseEntity<Void> completeLeagueRoom(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        leagueRoomService.completeLeagueRoom(roomId, currentUser);
        return ResponseEntity.ok().build();
    }

    // <--- 이 부분 수정 시작 ---
    @PostMapping("/{roomId}/advance-to-finals")
    public ResponseEntity<Void> advanceToFinals(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        LeagueRoom leagueRoom = leagueRoomService.getLeagueRoomById(roomId);
        if (!leagueRoom.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("리그 운영자만 본선을 생성할 수 있습니다.");
        }

        matchService.advanceToFinals(roomId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/next-round")
    public ResponseEntity<Void> generateNextRound(@PathVariable Long roomId) {
        matchService.generateNextRound(roomId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/auto-complete-preliminaries")
    public ResponseEntity<Void> autoCompletePreliminaries(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        LeagueRoom leagueRoom = leagueRoomService.getLeagueRoomById(roomId);
        if (!leagueRoom.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("리그 운영자만 경기를 자동 완료할 수 있습니다.");
        }
        matchService.autoCompleteGroupStage(roomId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/auto-complete-tournament-round")
    public ResponseEntity<Void> autoCompleteTournamentRound(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        LeagueRoom leagueRoom = leagueRoomService.getLeagueRoomById(roomId);
        if (!leagueRoom.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("리그 운영자만 경기를 자동 완료할 수 있습니다.");
        }
        matchService.autoCompleteTournamentRound(roomId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roomId}/final-results")
    public ResponseEntity<FinalResultDto> getFinalResults(@PathVariable Long roomId) {
        LeagueRoom leagueRoom = leagueRoomService.getLeagueRoomById(roomId);
        FinalResultDto finalResults = matchService.getFinalResults(leagueRoom);
        return ResponseEntity.ok(finalResults);
    }

    @DeleteMapping("/{roomId}/participants")
    public ResponseEntity<Void> leaveLeagueRoom(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        leagueRoomService.leaveLeagueRoom(roomId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{roomId}/participants/{participantId}")
    public ResponseEntity<Void> removeParticipant(@PathVariable Long roomId, @PathVariable Long participantId, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        leagueRoomService.removeParticipant(roomId, participantId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}/tournament-data")
    public ResponseEntity<TournamentDataDto> getTournamentData(@PathVariable Long roomId) {
        LeagueRoom leagueRoom = leagueRoomService.getLeagueRoomById(roomId);
        TournamentDataDto tournamentData = matchService.getTournamentData(leagueRoom);
        return ResponseEntity.ok(tournamentData);
    }

    @GetMapping("/{roomId}/group-details")
    public ResponseEntity<List<GroupDetailDto>> getGroupDetails(@PathVariable Long roomId) {
        LeagueRoom leagueRoom = leagueRoomService.getLeagueRoomById(roomId);
        List<Match> allMatches = matchService.getMatchesByLeagueRoom(leagueRoom);
        
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
        
        return ResponseEntity.ok(groupDetails);
    }

}
