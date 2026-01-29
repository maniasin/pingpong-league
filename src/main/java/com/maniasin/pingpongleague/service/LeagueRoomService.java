package com.maniasin.pingpongleague.service;

import com.maniasin.pingpongleague.domain.LeagueRoom;
import com.maniasin.pingpongleague.domain.LeagueRoomParticipant;
import com.maniasin.pingpongleague.domain.Match; // Match import 추가
import com.maniasin.pingpongleague.domain.User;
import com.maniasin.pingpongleague.dto.LeagueRoomUpdateRequestDto;
import com.maniasin.pingpongleague.repository.LeagueGroupRepository;
import com.maniasin.pingpongleague.repository.LeagueRoomParticipantRepository;
import com.maniasin.pingpongleague.repository.LeagueRoomRepository;
import com.maniasin.pingpongleague.repository.MatchRepository;
import com.maniasin.pingpongleague.repository.UserRepository;
import com.maniasin.pingpongleague.repository.LeagueTeamRepository;
import com.maniasin.pingpongleague.repository.LeagueTeamMemberRepository;
import com.maniasin.pingpongleague.dto.ParticipantAddFailureDto;
import com.maniasin.pingpongleague.dto.ParticipantBulkAddResponseDto;
import com.maniasin.pingpongleague.dto.TeamAssignmentRequestDto;
import com.maniasin.pingpongleague.dto.TeamCreateRequestDto;
import com.maniasin.pingpongleague.dto.TeamInfoDto;
import com.maniasin.pingpongleague.domain.LeagueTeam;
import com.maniasin.pingpongleague.domain.LeagueTeamMember;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LeagueRoomService {

    private static final Logger log = LoggerFactory.getLogger(LeagueRoomService.class);
    private final LeagueRoomRepository leagueRoomRepository;
    private final LeagueRoomParticipantRepository leagueRoomParticipantRepository;
    private final MatchRepository matchRepository;
    private final LeagueGroupRepository leagueGroupRepository; // <<< 이 한 줄을 추가해주세요!
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LeagueTeamRepository leagueTeamRepository;
    private final LeagueTeamMemberRepository leagueTeamMemberRepository;

    // ... (기존 메소드들은 변경 없이 그대로 둡니다) ...

    public Long createLeagueRoom(String title, User owner, int maxParticipants, LeagueRoom.GameType gameType, LeagueRoom.MatchFormat matchFormat, LeagueRoom.RoundRobinRankingType roundRobinRankingType, LeagueRoom.TournamentType tournamentType, Integer teamSize, LeagueRoom.TeamMatchFormat teamMatchFormat, String location, String venueAddress, String matchDescription, String contactInfo, int playersPerGroup, int advancingPlayersPerGroup, String eventDate, String eventTime) {
        log.info("새로운 리그 방 생성을 시작합니다. title: {}", title);

        LocalDateTime eventDateTime = LocalDateTime.of(LocalDate.parse(eventDate), LocalTime.parse(eventTime));
        LeagueRoom.RoundRobinRankingType resolvedRankingType = roundRobinRankingType != null
                ? roundRobinRankingType
                : LeagueRoom.RoundRobinRankingType.POINTS;
        Integer resolvedTeamSize = null;
        LeagueRoom.TeamMatchFormat resolvedTeamFormat = null;
        if (gameType == LeagueRoom.GameType.TEAM) {
            resolvedTeamSize = teamSize != null ? teamSize : 2;
            if (teamMatchFormat != null) {
                resolvedTeamFormat = teamMatchFormat;
            } else {
                resolvedTeamFormat = resolvedTeamSize == 3
                        ? LeagueRoom.TeamMatchFormat.SINGLE_DOUBLE_SINGLE
                        : LeagueRoom.TeamMatchFormat.SINGLES_ONLY;
            }
        }

        LeagueRoom leagueRoom = LeagueRoom.builder()
                .title(title)
                .owner(owner)
                .maxParticipants(maxParticipants)
                .gameType(gameType)
                .matchFormat(matchFormat)
                .roundRobinRankingType(resolvedRankingType)
                .tournamentType(tournamentType)
                .status(LeagueRoom.RoomStatus.OPEN)
                .teamSize(resolvedTeamSize)
                .teamMatchFormat(resolvedTeamFormat)
                .location(location)
                .venueAddress(venueAddress)
                .matchDescription(matchDescription)
                .contactInfo(contactInfo)
                .playersPerGroup(playersPerGroup)
                .advancingPlayersPerGroup(advancingPlayersPerGroup)
                .eventDateTime(eventDateTime)
                .build();

        log.debug("리그 방 엔티티 생성 완료. owner: {}", owner.getUsername());

        LeagueRoom savedRoom = leagueRoomRepository.save(leagueRoom);

        log.info("리그 방 생성 완료. Room ID: {}", savedRoom.getId());

        return savedRoom.getId();
    }

    private void ensureOwner(LeagueRoom leagueRoom, User currentUser) {
        if (!leagueRoom.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("리그 운영자만 참가자를 추가할 수 있습니다.");
        }
    }

    private void ensureTeamRoom(LeagueRoom leagueRoom) {
        if (leagueRoom.getGameType() != LeagueRoom.GameType.TEAM) {
            throw new IllegalStateException("단체전 리그에서만 팀 구성이 가능합니다.");
        }
        if (leagueRoom.getStatus() != LeagueRoom.RoomStatus.OPEN) {
            throw new IllegalStateException("모집중인 리그에서만 팀 구성을 변경할 수 있습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<TeamInfoDto> getTeams(Long roomId, User currentUser) {
        LeagueRoom leagueRoom = getLeagueRoomById(roomId);
        ensureOwner(leagueRoom, currentUser);
        ensureTeamRoom(leagueRoom);

        List<LeagueTeam> teams = leagueTeamRepository.findByLeagueRoom(leagueRoom);
        List<TeamInfoDto> teamInfos = new ArrayList<>();
        for (LeagueTeam team : teams) {
            List<LeagueTeamMember> members = leagueTeamMemberRepository.findByTeam(team);
            teamInfos.add(new TeamInfoDto(team, members));
        }
        return teamInfos;
    }

    public List<TeamInfoDto> saveTeams(Long roomId, User currentUser, TeamAssignmentRequestDto requestDto) {
        LeagueRoom leagueRoom = getLeagueRoomById(roomId);
        ensureOwner(leagueRoom, currentUser);
        ensureTeamRoom(leagueRoom);

        List<TeamCreateRequestDto> teams = requestDto != null ? requestDto.getTeams() : null;
        if (teams == null || teams.isEmpty()) {
            throw new IllegalArgumentException("팀 구성을 입력해주세요.");
        }

        int teamSize = leagueRoom.getTeamSize() != null ? leagueRoom.getTeamSize() : 2;
        List<User> participants = leagueRoom.getParticipants().stream()
                .map(LeagueRoomParticipant::getUser)
                .toList();
        if (participants.isEmpty()) {
            throw new IllegalStateException("참가자가 없습니다.");
        }

        validateTeamAssignments(teams, participants, teamSize);
        clearTeams(leagueRoom);

        List<TeamInfoDto> saved = new ArrayList<>();
        int teamIndex = 1;
        for (TeamCreateRequestDto teamDto : teams) {
            String name = teamDto.getName();
            if (name == null || name.isBlank()) {
                name = "팀 " + teamIndex;
            }
            LeagueTeam team = leagueTeamRepository.save(LeagueTeam.builder()
                    .leagueRoom(leagueRoom)
                    .name(name.trim())
                    .build());
            for (Long memberId : teamDto.getMemberIds()) {
                User member = userRepository.findById(memberId)
                        .orElseThrow(() -> new IllegalArgumentException("참가자를 찾을 수 없습니다."));
                LeagueTeamMember teamMember = LeagueTeamMember.builder()
                        .team(team)
                        .user(member)
                        .build();
                leagueTeamMemberRepository.save(teamMember);
            }
            saved.add(new TeamInfoDto(team, leagueTeamMemberRepository.findByTeam(team)));
            teamIndex++;
        }
        return saved;
    }

    public List<TeamInfoDto> autoAssignTeams(Long roomId, User currentUser) {
        LeagueRoom leagueRoom = getLeagueRoomById(roomId);
        ensureOwner(leagueRoom, currentUser);
        ensureTeamRoom(leagueRoom);

        List<User> participants = leagueRoom.getParticipants().stream()
                .map(LeagueRoomParticipant::getUser)
                .collect(Collectors.toCollection(ArrayList::new));
        if (participants.isEmpty()) {
            throw new IllegalStateException("참가자가 없습니다.");
        }

        clearTeams(leagueRoom);

        Collections.shuffle(participants);

        int teamSize = leagueRoom.getTeamSize() != null ? leagueRoom.getTeamSize() : 2;
        List<TeamInfoDto> saved = new ArrayList<>();
        int teamCount = 0;
        for (int i = 0; i < participants.size(); i += teamSize) {
            List<User> members = participants.subList(i, Math.min(i + teamSize, participants.size()));
            if (members.size() < teamSize) {
                throw new IllegalStateException("팀 인원이 부족합니다. 참가자 수를 확인해주세요.");
            }
            teamCount++;
            LeagueTeam team = leagueTeamRepository.save(LeagueTeam.builder()
                    .leagueRoom(leagueRoom)
                    .name("팀 " + teamCount)
                    .build());
            for (User member : members) {
                LeagueTeamMember teamMember = LeagueTeamMember.builder()
                        .team(team)
                        .user(member)
                        .build();
                leagueTeamMemberRepository.save(teamMember);
            }
            saved.add(new TeamInfoDto(team, leagueTeamMemberRepository.findByTeam(team)));
        }
        return saved;
    }

    private void validateTeamAssignments(List<TeamCreateRequestDto> teams, List<User> participants, int teamSize) {
        Set<Long> participantIds = participants.stream().map(User::getId).collect(Collectors.toSet());
        Set<Long> assignedIds = new HashSet<>();

        for (TeamCreateRequestDto team : teams) {
            List<Long> memberIds = team.getMemberIds();
            if (memberIds == null || memberIds.size() != teamSize) {
                throw new IllegalArgumentException("각 팀의 인원수를 맞춰주세요.");
            }
            for (Long memberId : memberIds) {
                if (!participantIds.contains(memberId)) {
                    throw new IllegalArgumentException("팀 구성에 참가하지 않은 사용자가 포함되어 있습니다.");
                }
                if (!assignedIds.add(memberId)) {
                    throw new IllegalArgumentException("같은 참가자가 여러 팀에 포함되어 있습니다.");
                }
            }
        }

        if (assignedIds.size() != participantIds.size()) {
            throw new IllegalArgumentException("모든 참가자를 팀에 배정해주세요.");
        }
    }

    private void clearTeams(LeagueRoom leagueRoom) {
        List<LeagueTeam> existingTeams = leagueTeamRepository.findByLeagueRoom(leagueRoom);
        for (LeagueTeam team : existingTeams) {
            leagueTeamMemberRepository.deleteByTeam(team);
        }
        leagueTeamRepository.deleteByLeagueRoom(leagueRoom);
    }

    public User addParticipantByIdentifier(Long roomId, User currentUser, String identifier) {
        LeagueRoom leagueRoom = getLeagueRoomById(roomId);
        ensureOwner(leagueRoom, currentUser);
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임 또는 아이디를 입력해주세요.");
        }

        String trimmed = identifier.trim();
        User target = userRepository.findByUsername(trimmed)
                .orElseGet(() -> userRepository.findByNickname(trimmed)
                        .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다.")));

        joinLeagueRoom(leagueRoom, target);
        return target;
    }

    public ParticipantBulkAddResponseDto addParticipantsBulk(Long roomId, User currentUser, List<String> identifiers) {
        LeagueRoom leagueRoom = getLeagueRoomById(roomId);
        ensureOwner(leagueRoom, currentUser);
        if (identifiers == null || identifiers.isEmpty()) {
            throw new IllegalArgumentException("추가할 닉네임/아이디가 없습니다.");
        }

        List<String> added = new ArrayList<>();
        List<ParticipantAddFailureDto> failed = new ArrayList<>();

        for (String raw : identifiers) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String identifier = raw.trim();
            try {
                User target = userRepository.findByUsername(identifier)
                        .orElseGet(() -> userRepository.findByNickname(identifier)
                                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다.")));
                joinLeagueRoom(leagueRoom, target);
                added.add(target.getNickname());
            } catch (Exception e) {
                failed.add(ParticipantAddFailureDto.builder()
                        .identifier(identifier)
                        .reason(e.getMessage())
                        .build());
            }
        }

        return ParticipantBulkAddResponseDto.builder()
                .added(added)
                .failed(failed)
                .build();
    }

    public User addGuestParticipant(Long roomId, User currentUser, String nickname) {
        LeagueRoom leagueRoom = getLeagueRoomById(roomId);
        ensureOwner(leagueRoom, currentUser);

        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("게스트 닉네임을 입력해주세요.");
        }
        String trimmed = nickname.trim();
        if (userRepository.existsByNickname(trimmed)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        String uniqueToken = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String username = "guest_" + roomId + "_" + uniqueToken;
        String phone = "GUEST-" + uniqueToken;
        String password = passwordEncoder.encode(UUID.randomUUID().toString());

        User guest = User.builder()
                .username(username)
                .password(password)
                .nickname(trimmed)
                .name(trimmed)
                .phone(phone)
                .build();

        userRepository.save(guest);
        joinLeagueRoom(leagueRoom, guest);
        return guest;
    }

    @Transactional(readOnly = true)
    public List<LeagueRoom> getAllLeagueRooms() {
        return leagueRoomRepository.findAllWithOwner();
    }

    @Transactional(readOnly = true)
    public LeagueRoom getLeagueRoomById(Long roomId) {
        return leagueRoomRepository.findByIdWithDetails(roomId)
                .orElseThrow(() -> new IllegalArgumentException("리그 방을 찾을 수 없습니다."));
    }

    private void joinLeagueRoom(LeagueRoom leagueRoom, User user) {
        if (leagueRoom.getStatus() != LeagueRoom.RoomStatus.OPEN) {
            throw new IllegalStateException("모집중인 리그가 아닙니다.");
        }
        // contains 로직을 위해 User 객체에 EqualsAndHashCode가 구현되어 있어야 합니다.
        boolean alreadyJoined = leagueRoom.getParticipants().stream()
                .anyMatch(p -> p.getUser().equals(user));
        if (alreadyJoined) {
            throw new IllegalArgumentException("이미 참가한 리그입니다.");
        }
        if (leagueRoom.getParticipants().size() >= leagueRoom.getMaxParticipants()) {
            throw new IllegalStateException("리그 방의 최대 참가 인원에 도달했습니다.");
        }
        LeagueRoomParticipant participant = LeagueRoomParticipant.builder()
                .leagueRoom(leagueRoom)
                .user(user)
                .build();
        leagueRoomParticipantRepository.save(participant);
        leagueRoom.getParticipants().add(participant); // 영속성 컨텍스트 상태를 일관성 있게 유지
    }

    // ▼▼▼ 3. 외부에서 호출할 joinLeagueRoom 메소드를 새로 만듭니다. ▼▼▼
    public void joinLeagueRoom(Long roomId, User user) {
        // 이 메소드는 DB에서 방 정보를 한번만 조회합니다.
        LeagueRoom leagueRoom = getLeagueRoomById(roomId);
        joinLeagueRoom(leagueRoom, user); // 실제 로직은 위 메소드에 위임
    }

    public void deleteLeagueRoom(Long roomId, User currentUser) {
        LeagueRoom leagueRoom = getLeagueRoomById(roomId);
        if (!leagueRoom.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("리그 운영자만 방을 삭제할 수 있습니다.");
        }

        // <<< 핵심 수정 시작 >>>
        // 1. 이 방에 속한 모든 경기(Match)를 삭제합니다.
        matchRepository.deleteByLeagueRoom(leagueRoom);
        // 2. 이 방에 속한 모든 조(Group)를 삭제합니다.
        leagueGroupRepository.deleteByLeagueRoom(leagueRoom);
        // 2-1. 이 방에 속한 모든 팀을 삭제합니다.
        leagueTeamRepository.deleteByLeagueRoom(leagueRoom);
        // 3. 참가자 목록을 비워주어 관계를 끊습니다 (CascadeType.ALL과 orphanRemoval=true에 의해 처리되지만, 명시적으로 수행하는 것이 더 안전합니다).
        leagueRoom.getParticipants().clear();

        // 4. 이제 안전하게 리그 방을 삭제합니다.
        leagueRoomRepository.delete(leagueRoom);
        log.info("리그 방 삭제 완료. Room ID: {}", roomId);
        // <<< 핵심 수정 끝 >>>
    }

    public void reopenLeagueRoom(Long roomId, User currentUser) {
        LeagueRoom leagueRoom = getLeagueRoomById(roomId);
        if (!leagueRoom.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("리그 운영자만 상태를 변경할 수 있습니다.");
        }
        if (leagueRoom.getStatus() != LeagueRoom.RoomStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행중인 리그만 다시 모집할 수 있습니다.");
        }

        // <<< 핵심 수정 시작 >>>
        matchRepository.deleteByLeagueRoom(leagueRoom);
        leagueGroupRepository.deleteByLeagueRoom(leagueRoom); // 조 정보도 함께 삭제해야 합니다.
        leagueTeamRepository.deleteByLeagueRoom(leagueRoom);
        log.info("기존 대진표 및 조 정보 삭제 완료. Room ID: {}", roomId);
        // <<< 핵심 수정 끝 >>>

        leagueRoom.setStatus(LeagueRoom.RoomStatus.OPEN);
        leagueRoomRepository.save(leagueRoom);
        log.info("리그 상태를 '모집중'으로 변경 완료. Room ID: {}", roomId);
    }

    public void updateLeagueRoom(Long roomId, User currentUser, LeagueRoomUpdateRequestDto requestDto) {
        LeagueRoom leagueRoom = getLeagueRoomById(roomId);
        if (!leagueRoom.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("리그 운영자만 방 설정을 수정할 수 있습니다.");
        }
        if (leagueRoom.getStatus() != LeagueRoom.RoomStatus.OPEN) {
            throw new IllegalStateException("모집중인 리그만 수정할 수 있습니다.");
        }
        leagueRoom.update(requestDto);
    }

    // 리그 완료 처리 메소드 추가
    public void completeLeagueRoom(Long roomId, User currentUser) {
        LeagueRoom leagueRoom = getLeagueRoomById(roomId);
        if (!leagueRoom.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("리그 운영자만 리그를 종료할 수 있습니다.");
        }
        if (leagueRoom.getStatus() != LeagueRoom.RoomStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행중인 리그만 종료할 수 있습니다.");
        }
        // 아직 진행중인 경기가 있는지 확인
        boolean hasPendingMatches = matchRepository.existsByLeagueRoomAndStatus(leagueRoom, Match.MatchStatus.PENDING);
        if (hasPendingMatches) {
            throw new IllegalStateException("아직 모든 경기가 종료되지 않았습니다.");
        }
        leagueRoom.setStatus(LeagueRoom.RoomStatus.COMPLETED);
        leagueRoomRepository.save(leagueRoom);
        log.info("리그가 성공적으로 종료되었습니다. Room ID: {}", roomId);
    }

    public void leaveLeagueRoom(Long roomId, User user) {
        LeagueRoom leagueRoom = getLeagueRoomById(roomId);
        if (leagueRoom.getStatus() != LeagueRoom.RoomStatus.OPEN) {
            throw new IllegalStateException("모집중인 리그만 참가를 취소할 수 있습니다.");
        }

        LeagueRoomParticipant participant = leagueRoomParticipantRepository.findByLeagueRoomIdAndUserId(roomId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("이 리그에 참가하지 않았습니다."));

        leagueRoomParticipantRepository.delete(participant);
        log.info("사용자 '{}'가 리그 참가를 취소했습니다. Room ID: {}", user.getUsername(), roomId);
    }

    public void removeParticipant(Long roomId, Long participantUserId, User currentUser) {
        LeagueRoom leagueRoom = getLeagueRoomById(roomId);
        if (!leagueRoom.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("리그 운영자만 참가자를 삭제할 수 있습니다.");
        }
        if (leagueRoom.getStatus() != LeagueRoom.RoomStatus.OPEN) {
            throw new IllegalStateException("모집중인 리그에서만 참가자를 삭제할 수 있습니다.");
        }
        if (leagueRoom.getOwner().getId().equals(participantUserId)) {
            throw new IllegalArgumentException("리그 운영자는 스스로를 삭제할 수 없습니다.");
        }

        LeagueRoomParticipant participant = leagueRoomParticipantRepository.findByLeagueRoomIdAndUserId(roomId, participantUserId)
                .orElseThrow(() -> new IllegalArgumentException("해당 참가자를 찾을 수 없습니다."));

        leagueRoomParticipantRepository.delete(participant);
        log.info("운영자가 참가자(ID:{})를 리그에서 삭제했습니다. Room ID: {}", participantUserId, roomId);
    }

}
