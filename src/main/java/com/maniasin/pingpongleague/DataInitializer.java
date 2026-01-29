package com.maniasin.pingpongleague;

import com.maniasin.pingpongleague.domain.LeagueRoom;
import com.maniasin.pingpongleague.domain.User;
import com.maniasin.pingpongleague.repository.UserRepository;
import com.maniasin.pingpongleague.service.LeagueRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Profile("dev")
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LeagueRoomService leagueRoomService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (userRepository.count() == 0) {
            System.out.println("테스트용 가상 선수 데이터 생성을 시작합니다...");
            List<User> users = new ArrayList<>();
            for (int i = 1; i <= 60; i++) {  // 50명 -> 60명으로 증가
                if(i == 1)
                {
                    User user = User.builder()
                            .username("user" + i)
                            .password(passwordEncoder.encode("password"))
                            .name("신동진")
                            .nickname("신동진")
                            .phone("010-0000-" + String.format("%04d", i))
                            .build();
                    users.add(user);
                }
                else if(i == 2)
                {
                    User user = User.builder()
                            .username("user" + i)
                            .password(passwordEncoder.encode("password"))
                            .name("박신용")
                            .nickname("박신용")
                            .phone("010-0000-" + String.format("%04d", i))
                            .build();
                    users.add(user);
                }
                else if(i == 3)
                {
                    User user = User.builder()
                            .username("user" + i)
                            .password(passwordEncoder.encode("password"))
                            .name("구자익")
                            .nickname("구자익")
                            .phone("010-0000-" + String.format("%04d", i))
                            .build();
                    users.add(user);
                }
                else if(i == 4)
                {
                    User user = User.builder()
                            .username("user" + i)
                            .password(passwordEncoder.encode("password"))
                            .name("전성민")
                            .nickname("전성민")
                            .phone("010-0000-" + String.format("%04d", i))
                            .build();
                    users.add(user);
                }
                else {
                    User user = User.builder()
                            .username("user" + i)
                            .password(passwordEncoder.encode("password"))
                            .name("선수" + i)
                            .nickname("닉네임" + i)
                            .phone("010-0000-" + String.format("%04d", i))
                            .build();
                    users.add(user);
                }
            }
            userRepository.saveAll(users);
            System.out.println("가상 선수 60명 데이터 생성 완료.");
        }

        if (userRepository.findByUsername("owner").isEmpty()) {
            System.out.println("테스트용 샘플 리그 및 참가 데이터 생성을 시작합니다...");

            User owner = User.builder()
                    .username("owner")
                    .password(passwordEncoder.encode("1234"))
                    .name("방장")
                    .nickname("리그운영자")
                    .phone("010-1234-5678")
                    .build();
            userRepository.save(owner);

            // 1. 첫 번째 리그 생성
            Long roundRobinRoomId = leagueRoomService.createLeagueRoom(
                    "샘플 조별 풀리그",
                    owner,
                    20,
                    com.maniasin.pingpongleague.domain.LeagueRoom.GameType.SINGLE,
                    com.maniasin.pingpongleague.domain.LeagueRoom.MatchFormat.ROUND_ROBIN,
                    com.maniasin.pingpongleague.domain.LeagueRoom.RoundRobinRankingType.POINTS,
                    null, // 풀리그는 토너먼트 타입 불필요
                    null,
                    null,
                    "서울 강남구",
                    "서울 강남구 테헤란로 123",
                    "예선 풀리그 후 상위 진출",
                    "010-1111-2222",
                    5,
                    4, // 조별 상위 2명
                    "2025-07-15",
                    "19:00"
            );

            // 2. 첫 번째 리그에 참가할 선수 목록 준비
            List<User> playersForRoundRobin = userRepository.findAll().stream()
                    .filter(user -> user.getUsername().startsWith("user"))
                    .limit(20)
                    .toList();

            // 3. 선수들을 리그에 참가시킴
            for (User player : playersForRoundRobin) {
                try {
                    leagueRoomService.joinLeagueRoom(roundRobinRoomId, player);
                } catch (Exception e) {
                    System.err.println(player.getUsername() + " 참가 중 오류 발생: " + e.getMessage());
                }
            }
            System.out.println("샘플 조별 풀리그에 선수 16명 참가 완료.");

            // 4. 두 번째 리그 생성 (단방향 토너먼트)
            Long tournamentRoomId = leagueRoomService.createLeagueRoom(
                    "📊 단방향 토너먼트 (20명)",
                    owner,
                    20,
                    com.maniasin.pingpongleague.domain.LeagueRoom.GameType.SINGLE,
                    com.maniasin.pingpongleague.domain.LeagueRoom.MatchFormat.PRELIMINARY_TOURNAMENT,
                    com.maniasin.pingpongleague.domain.LeagueRoom.RoundRobinRankingType.POINTS,
                    com.maniasin.pingpongleague.domain.LeagueRoom.TournamentType.STANDARD, // 단방향 토너먼트
                    null,
                    null,
                    "경기 부천시",
                    "경기 부천시 중동로 45",
                    "본선 단방향 토너먼트",
                    "010-2222-3333",
                    5,
                    2, // 조별 상위 2명 본선 진출
                    "2025-07-20",
                    "10:00"
            );

            // 5. 두 번째 리그에 참가할 선수 목록 준비
            List<User> playersForTournament = userRepository.findAll().stream()
                    .filter(user -> user.getUsername().startsWith("user"))
                    .limit(20)
                    .toList();

            // 6. 선수들을 리그에 참가시킴
            for (User player : playersForTournament) {
                try {
                    leagueRoomService.joinLeagueRoom(tournamentRoomId, player);
                } catch (Exception e) {
                    System.err.println(player.getUsername() + " 참가 중 오류 발생: " + e.getMessage());
                }
            }
            System.out.println("📊 단방향 토너먼트에 선수 20명 참가 완료.");

            // 7. 세 번째 리그 생성 (양방향 토너먼트)
            Long splitTournamentRoomId = leagueRoomService.createLeagueRoom(
                    "⚔️ 양방향 토너먼트 (16명)",
                    owner,
                    16,
                    com.maniasin.pingpongleague.domain.LeagueRoom.GameType.SINGLE,
                    com.maniasin.pingpongleague.domain.LeagueRoom.MatchFormat.PRELIMINARY_TOURNAMENT,
                    com.maniasin.pingpongleague.domain.LeagueRoom.RoundRobinRankingType.POINTS,
                    com.maniasin.pingpongleague.domain.LeagueRoom.TournamentType.SPLIT, // 양방향 토너먼트
                    null,
                    null,
                    "인천 부평구",
                    "인천 부평구 경원대로 100",
                    "양방향 대진표 운영",
                    "010-3333-4444",
                    4, // 조별 인원 4명
                    2, // 조별 상위 2명 본선 진출 (4조 × 2명 = 8명 본선)
                    "2025-07-25",
                    "14:00"
            );

            // 8. 세 번째 리그에 참가할 선수 목록 준비
            List<User> playersForSplitTournament = userRepository.findAll().stream()
                    .filter(user -> user.getUsername().startsWith("user"))
                    .skip(20) // 앞의 20명은 건너뛰고 다른 선수들로
                    .limit(16)
                    .toList();

            // 9. 선수들을 리그에 참가시킴
            for (User player : playersForSplitTournament) {
                try {
                    leagueRoomService.joinLeagueRoom(splitTournamentRoomId, player);
                } catch (Exception e) {
                    System.err.println(player.getUsername() + " 참가 중 오류 발생: " + e.getMessage());
                }
            }
            System.out.println("⚔️ 양방향 토너먼트에 선수 16명 참가 완료.");

            // 10. 네 번째 리그 생성 (대규모 양방향 토너먼트)
            Long largeTournamentRoomId = leagueRoomService.createLeagueRoom(
                    "⚔️ 대규모 양방향 토너먼트 (40명)",
                    owner,
                    40,
                    com.maniasin.pingpongleague.domain.LeagueRoom.GameType.SINGLE,
                    com.maniasin.pingpongleague.domain.LeagueRoom.MatchFormat.PRELIMINARY_TOURNAMENT,
                    com.maniasin.pingpongleague.domain.LeagueRoom.RoundRobinRankingType.POINTS,
                    com.maniasin.pingpongleague.domain.LeagueRoom.TournamentType.SPLIT, // 양방향 토너먼트
                    null,
                    null,
                    "서울 송파구",
                    "서울 송파구 올림픽로 10",
                    "대규모 양방향 토너먼트",
                    "010-4444-5555",
                    8, // 조별 인원 8명
                    2, // 조별 상위 2명 본선 진출 (5조 × 2명 = 10명 본선)
                    "2025-08-01",
                    "14:00"
            );

            // 11. 네 번째 리그에 참가할 선수 목록 준비 (40명)
            List<User> playersForLargeTournament = userRepository.findAll().stream()
                    .filter(user -> user.getUsername().startsWith("user"))
                    .limit(40)
                    .toList();

            // 12. 선수들을 리그에 참가시킴
            for (User player : playersForLargeTournament) {
                try {
                    leagueRoomService.joinLeagueRoom(largeTournamentRoomId, player);
                } catch (Exception e) {
                    System.err.println(player.getUsername() + " 참가 중 오류 발생: " + e.getMessage());
                }
            }
            System.out.println("⚔️ 대규모 양방향 토너먼트에 선수 40명 참가 완료.");

            // 13. 다섯 번째 리그 생성 (60명 전원 토너먼트 진출)
            Long fullTournamentRoomId = leagueRoomService.createLeagueRoom(
                    "⚔️ 대규모 전원 토너먼트 (60명)",
                    owner,
                    60,
                    com.maniasin.pingpongleague.domain.LeagueRoom.GameType.SINGLE,
                    com.maniasin.pingpongleague.domain.LeagueRoom.MatchFormat.PRELIMINARY_TOURNAMENT,
                    com.maniasin.pingpongleague.domain.LeagueRoom.RoundRobinRankingType.POINTS,
                    com.maniasin.pingpongleague.domain.LeagueRoom.TournamentType.SPLIT, // 양방향 토너먼트
                    null,
                    null,
                    "경기 수원시",
                    "경기 수원시 영통로 77",
                    "전원 토너먼트 진행",
                    "010-5555-6666",
                    10, // 조별 인원 10명 (6개조)
                    10, // 조별 전원 본선 진출
                    "2025-08-10",
                    "10:00"
            );

            // 14. 다섯 번째 리그에 참가할 선수 목록 준비 (60명 전원)
            List<User> playersForFullTournament = userRepository.findAll().stream()
                    .filter(user -> user.getUsername().startsWith("user"))
                    .limit(60)
                    .toList();

            // 15. 선수들을 리그에 참가시킴
            for (User player : playersForFullTournament) {
                try {
                    leagueRoomService.joinLeagueRoom(fullTournamentRoomId, player);
                } catch (Exception e) {
                    System.err.println(player.getUsername() + " 참가 중 오류 발생: " + e.getMessage());
                }
            }
            System.out.println("⚔️ 대규모 전원 토너먼트에 선수 60명 참가 완료.");
        }
    }
}
