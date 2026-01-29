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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MyttCrawler implements SiteCrawler {

    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;
    private final AwardRecordRepository awardRecordRepository;

    @Override
    public String getSiteName() {
        return "MyTT";
    }

    @Override
    public void scrape(String playerName) throws Exception {
        WebDriverManager.chromedriver().setup();
        log.info("[{}] 크롤링 시작: {}", getSiteName(), playerName);

        Player player = playerRepository.findByName(playerName)
                .orElseGet(() -> playerRepository.save(Player.builder().name(playerName).build()));

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox"); // <-- 이 줄 추가
        options.addArguments("--disable-dev-shm-usage"); // <-- 이 줄 추가
        options.addArguments("--disable-gpu"); // <-- 이 줄 추가
        options.addArguments("--lang=ko-KR");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        int savedCount = 0;

        try {
            driver.get("https://mytt.kr/main/winner_list.xhtml");
            log.info("[{}] 입상자 검색 페이지 접속 성공", getSiteName());

            // 페이지의 기본 요소들이 로드될 때까지 대기
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("mainForm:playerName")));

            // ▼▼▼▼▼ [핵심 수정 1] 검색 전, 초기 테이블의 첫 행을 미리 잡아둡니다. ▼▼▼▼▼
            WebElement initialFirstRow = driver.findElement(By.cssSelector("#mainForm\\:winnerTable_data > tr"));

            WebElement nameInput = driver.findElement(By.id("mainForm:playerName"));
            nameInput.sendKeys(playerName);
            log.info("[{}] 검색어 '{}' 입력 완료", getSiteName(), playerName);

            WebElement searchButton = driver.findElement(By.id("mainForm:j_idt84"));
            searchButton.click();
            log.info("[{}] 검색 버튼 클릭 완료", getSiteName());

            // ▼▼▼▼▼ [핵심 수정 2] 검색 전 잡아뒀던 첫 행이 사라질 때(stale)까지 기다립니다. ▼▼▼▼▼
            wait.until(ExpectedConditions.stalenessOf(initialFirstRow));
            log.info("[{}] 검색 결과(AJAX) 로딩 완료", getSiteName());

            // 페이지네이션 루프
            while (true) {
                List<WebElement> seleniumRows = driver.findElements(By.cssSelector("#mainForm\\:winnerTable_data > tr"));
                if (seleniumRows.isEmpty() || (seleniumRows.size() == 1 && seleniumRows.get(0).getText().contains("입상자 내역이 없습니다."))) {
                    log.info("[{}] 검색 결과가 없습니다.", getSiteName());
                    break;
                }
                WebElement firstRowOfCurrentPage = seleniumRows.get(0);

                Document doc = Jsoup.parse(driver.getPageSource());
                Elements jsoupRows = doc.select("#mainForm\\:winnerTable_data > tr");

                for (Element row : jsoupRows) {
                    Elements cells = row.select("td");
                    if (cells.size() < 7) continue;

                    String tournamentName = cells.get(0).text();
                    String dateStr = cells.get(1).text();
                    String division = cells.get(3).text();
                    String placing = cells.get(4).text();
                    String detail = cells.get(6).text();

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    LocalDate tournamentDate = LocalDate.parse(dateStr, formatter);

                    Tournament tournament = tournamentRepository.findByNameAndTournamentDate(tournamentName, tournamentDate)
                            .orElseGet(() -> tournamentRepository.save(Tournament.builder()
                                    .name(tournamentName).tournamentDate(tournamentDate).organizer(getSiteName()).build()));

                    if (!awardRecordRepository.existsByPlayerIdAndTournamentIdAndDivisionAndPlacing(
                            player.getId(), tournament.getId(), division, placing)) {
                        AwardRecord record = AwardRecord.builder()
                                .player(player).tournament(tournament).division(division).detail(detail).placing(placing)
                                .build();
                        awardRecordRepository.save(record);
                        savedCount++;
                    }
                }

                // '다음' 페이지 버튼 상태 확인 및 클릭
                try {
                    WebElement nextPageButton = driver.findElement(By.cssSelector("span.ui-paginator-next"));
                    if (nextPageButton.getAttribute("class").contains("ui-state-disabled")) {
                        log.info("[{}] 마지막 페이지입니다. 크롤링을 종료합니다.", getSiteName());
                        break;
                    }
                    nextPageButton.click();
                    wait.until(ExpectedConditions.stalenessOf(firstRowOfCurrentPage));
                    log.info("[{}] 다음 페이지 로딩 완료.", getSiteName());
                } catch (NoSuchElementException e) {
                    log.info("[{}] '다음' 버튼을 찾을 수 없습니다. 단일 페이지입니다.", getSiteName());
                    break;
                } catch(Exception e){
                    log.warn("[{}] 페이지 전환 중 오류 발생. 마지막 페이지로 간주합니다.", getSiteName());
                    break;
                }
            }
        } finally {
            driver.quit();
        }
        log.info("[{}] 크롤링 완료: {}개의 새로운 기록 저장됨", getSiteName(), savedCount);
    }
}