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
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KoreaTakguCrawler implements SiteCrawler {

    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;
    private final AwardRecordRepository awardRecordRepository;

    @Override
    public String getSiteName() {
        return "코리아탁구";
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
            driver.get("http://www.koreatakgu.com/seoul/2017/Do.jsp?urlSeq=302");
            log.info("[{}] 개인별 결과 페이지 접속 성공", getSiteName());

            WebElement nameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("userNm")));
            nameInput.sendKeys(playerName);

            WebElement searchButton = driver.findElement(By.cssSelector("a._btn[href='javascript:document.searchForm.submit()']"));
            searchButton.click();

            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//td[contains(text(), '대회일자')]")));
            log.info("[{}] 검색 결과 페이지 로딩 확인", getSiteName());

            Document doc = Jsoup.parse(driver.getPageSource());
            Elements rows = doc.select("div:contains(탁구대회 성적) + table tr");

            // ▼▼▼▼▼▼▼ 필터링할 입상 성적 리스트 정의 ▼▼▼▼▼▼▼
            List<String> prizedPlacings = List.of("우승", "준우승", "4강");

            for (Element row : rows) {
                if (row.text().contains("대회일자")) {
                    continue;
                }

                Elements cells = row.select("td");
                if (cells.size() < 5) continue;

                try {
                    String placing = cells.get(4).text().trim();

                    // ▼▼▼▼▼▼▼ 입상 성적인지 확인하는 필터링 로직 추가 ▼▼▼▼▼▼▼
                    if (!prizedPlacings.contains(placing)) {
                        continue; // 우승, 준우승, 4강이 아니면 다음 기록으로 건너뛰기
                    }

                    String dateStr = cells.get(1).text().trim();
                    String tournamentName = cells.get(2).text().trim();
                    String division = cells.get(3).text().trim();
                    String detail = "";

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
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
                        log.info("[{}] 새로운 입상 기록 저장: {} | {} | {} | {}",
                                getSiteName(), tournamentName, division, detail, placing);
                    }
                } catch (DateTimeParseException e) {
                    log.warn("[{}] 날짜 파싱 실패: '{}'", getSiteName(), cells.get(1).text());
                } catch (Exception e) {
                    log.error("[{}] 행 처리 중 오류 발생: {}", getSiteName(), row.html(), e);
                }
            }
        } finally {
            driver.quit();
        }
        log.info("[{}] 크롤링 완료: {}개의 새로운 기록 저장됨", getSiteName(), savedCount);
    }
}