package com.maniasin.pingpongleague.service.crawler;

import com.maniasin.pingpongleague.domain.AwardRecord;
import com.maniasin.pingpongleague.domain.Player;
import com.maniasin.pingpongleague.domain.Tournament;
import com.maniasin.pingpongleague.repository.AwardRecordRepository;
import com.maniasin.pingpongleague.repository.PlayerRepository;
import com.maniasin.pingpongleague.repository.TournamentRepository;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils; // FileUtils import
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File; // File import
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AirpingCrawler implements SiteCrawler {

    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;
    private final AwardRecordRepository awardRecordRepository;

    @Override
    public String getSiteName() {
        return "에어핑";
    }

    @Override
    public void scrape(String playerName) throws Exception {
        WebDriverManager.chromedriver().setup();

        log.info("[{}] 크롤링 시작: {}", getSiteName(), playerName);

        Player player = playerRepository.findByName(playerName)
                .orElseGet(() -> playerRepository.save(Player.builder().name(playerName).build()));

        // --- Selenium WebDriver 설정 ---
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // 로컬 테스트 시에는 주석 처리하여 브라우저 창 확인
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--lang=ko-KR"); // 브라우저 언어를 한국어로 설정
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        String pageSource;

        try {
            // 1. 검색 페이지로 이동
            String baseUrl = "https://www.airping.co.kr/11player/01.php";
            driver.get(baseUrl);
            log.info("[{}] 기본 검색 페이지 접속: {}", getSiteName(), baseUrl);

            // 2. 검색창에 선수 이름 입력
            WebElement searchInput = driver.findElement(By.id("player_search_keyword"));
            searchInput.sendKeys(playerName);
            log.info("[{}] 검색어 '{}' 입력 완료", getSiteName(), playerName);

            // 3. 검색 버튼 클릭
            WebElement searchButton = driver.findElement(By.cssSelector(".player_search_btn"));
            searchButton.click();
            log.info("[{}] 검색 버튼 클릭 완료", getSiteName());

            // 4. 실제 결과가 로딩될 때까지 대기
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li._mc_div:not(._vc_fixed)")));
            log.info("[{}] 검색 결과 페이지 로딩 확인", getSiteName());

            // 5. "더보기" 버튼이 있다면 모두 클릭하여 모든 데이터 로드
            while (true) {
                try {
                    List<WebElement> moreButtons = driver.findElements(By.cssSelector("._cc_view_more_btn"));
                    if (moreButtons.isEmpty() || !moreButtons.get(0).isDisplayed()) {
                        log.info("[{}] 더 이상 '더보기' 버튼이 없거나 보이지 않아 로딩을 완료합니다.", getSiteName());
                        break;
                    }
                    WebElement buttonToClick = moreButtons.get(0);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", buttonToClick);
                    log.info("[{}] '더보기' 버튼 클릭", getSiteName());
                    Thread.sleep(1500); // AJAX 로딩 대기
                } catch (Exception e) {
                    log.warn("[{}] '더보기' 버튼 처리 중 오류: {}", getSiteName(), e.getMessage());
                    break;
                }
            }

            pageSource = driver.getPageSource();

        } catch (Exception e) {
            log.error("크롤링 중 예외 발생! 스크린샷을 저장합니다.");
            File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            try {
                FileUtils.copyFile(scrFile, new File("./error_screenshot.png"));
                log.info("스크린샷 저장 완료: ./error_screenshot.png");
            } catch (Exception ioException) {
                log.error("스크린샷 저장에 실패했습니다.", ioException);
            }
            throw e;
        } finally {
            driver.quit();
        }

        Document doc = Jsoup.parse(pageSource);
        log.debug("[{}] 페이지 소스 길이: {}", getSiteName(), pageSource.length());

        String[] selectors = {
                "._mc_result_div ul.player_cont_body > li._mc_div:not(._vc_fixed)",
                "._mc_result_div ul.player_cont_body > li._mc_div",
                "ul.player_cont_body > li._mc_div:not(._vc_fixed)",
                "ul.player_cont_body > li._mc_div",
                ".player_cont_body li._mc_div:not(._vc_fixed)",
                ".player_cont_body li._mc_div",
                "li._mc_div:not(._vc_fixed)",
                "li._mc_div"
        };

        Elements tournamentBlocks = null;
        for (String selector : selectors) {
            Elements found = doc.select(selector);
            if (!found.isEmpty()) {
                // placeholder가 아닌 실제 데이터가 있는 경우만 선택
                if (found.stream().anyMatch(el -> !el.hasClass("_vc_fixed"))) {
                    tournamentBlocks = found;
                    log.info("[{}] 선택자 '{}'로 {}개의 블록을 찾았습니다.", getSiteName(), selector, tournamentBlocks.size());
                    break;
                }
            }
        }

        if (tournamentBlocks == null || tournamentBlocks.isEmpty()) {
            log.warn("[{}] 실제 대회 정보 블록을 찾을 수 없습니다.", getSiteName());
            return;
        }

        int savedCount = 0;
        for (int i = 0; i < tournamentBlocks.size(); i++) {
            Element block = tournamentBlocks.get(i);

            if (block.hasClass("_vc_fixed") || block.select(".play_date_none").size() > 0 || block.select("._cc_view_more_btn").size() > 0) {
                log.debug("[{}] 블록 {} 건너뛰기 (빈 데이터 또는 더보기 버튼)", getSiteName(), i);
                continue;
            }

            // log.info("[{}] 블록 {} HTML: {}", getSiteName(), i, block.html());
            try {
                String tournamentName = block.select(".player_inner4 .player_box a").text().trim();
                if (tournamentName.isEmpty()) {
                    tournamentName = block.select(".player_inner4 .game_match_wrap").text().trim();
                }

                String dateText = block.select(".player_inner5 .player_box").text().trim();
                String dateStr = "";
                if (!dateText.isEmpty()) {
                    if (dateText.contains("~")) {
                        dateStr = dateText.split("~")[0].trim();
                    } else {
                        dateStr = dateText;
                    }
                }

                log.debug("[{}] 최종 대회명: '{}', 날짜: '{}'", getSiteName(), tournamentName, dateStr);

                if (tournamentName.isEmpty() || dateStr.isEmpty()) {
                    log.debug("[{}] 대회명 또는 날짜가 비어있어 건너뜁니다.", getSiteName());
                    continue;
                }

                final String finalTournamentName = tournamentName;
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
                final LocalDate tournamentDate = LocalDate.parse(dateStr, formatter);

                Tournament tournament = tournamentRepository.findByNameAndTournamentDate(finalTournamentName, tournamentDate)
                        .orElseGet(() -> tournamentRepository.save(Tournament.builder()
                                .name(finalTournamentName).tournamentDate(tournamentDate).organizer(getSiteName()).build()));

                Elements prizedRecords = block.select("li._mc_player_result._mc_res_prized");
                log.debug("[{}] 입상 기록 {}개 발견", getSiteName(), prizedRecords.size());

                for (Element recordRow : prizedRecords) {
                    String placing = recordRow.select(".game_result").text().trim();
                    String divisionText = recordRow.select(".sear_game_type").text().replace("[", "").replace("]", "").trim(); // "단체전"

                    Element tempRow = recordRow.clone();
                    tempRow.select(".game_result, .sear_game_type").remove(); // 성적과 종목 태그 모두 제거
                    String detailText = tempRow.text().trim(); // "지역 혼성 7~8부"

                    log.debug("[{}] 종목: {}, 상세부수: {}, 성적: {}", getSiteName(), divisionText, detailText, placing);

                    boolean recordExists = awardRecordRepository.existsByPlayerIdAndTournamentIdAndDivisionAndPlacing(
                            player.getId(), tournament.getId(), divisionText, placing); // 기존 검사 로직은 유지

                    if (!recordExists) {
                        AwardRecord record = AwardRecord.builder()
                                .player(player)
                                .tournament(tournament)
                                .division(divisionText) // 분리된 '종목' 저장
                                .detail(detailText)     // 분리된 '상세 부수' 저장
                                .placing(placing)
                                .build();
                        awardRecordRepository.save(record);

                        savedCount++;
                        log.info("[{}] 새로운 입상 기록 저장: {} | {} | {} | {} | {}",
                                getSiteName(), player.getName(), tournament.getName(), divisionText, detailText, placing);
                    } else {
                        log.debug("[{}] 이미 존재하는 기록: {} | {} | {} | {} | {}",
                                getSiteName(), player.getName(), tournament.getName(), divisionText, detailText, placing);
                    }
                }
            } catch (DateTimeParseException e) {
                log.error("[{}] 날짜 파싱 실패", getSiteName(), e);
            } catch (Exception e) {
                log.error("[{}] 블록 처리 중 오류", getSiteName(), e);
                throw e;
            }
        }
        log.info("[{}] 크롤링 완료: {}개의 새로운 기록 저장됨", getSiteName(), savedCount);
    }
}