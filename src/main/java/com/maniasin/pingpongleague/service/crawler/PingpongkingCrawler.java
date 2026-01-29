package com.maniasin.pingpongleague.service.crawler;

import com.maniasin.pingpongleague.config.AccountProperties;
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
import org.openqa.selenium.*;
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
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PingpongkingCrawler implements SiteCrawler {

    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;
    private final AwardRecordRepository awardRecordRepository;
    private final AccountProperties accountProperties;

    @Override
    public String getSiteName() {
        return "탁구왕";
    }

    @Override
    public void scrape(String playerName) {
        WebDriverManager.chromedriver().setup();
        log.info("[{}] 크롤링 시작: {}", getSiteName(), playerName);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--lang=ko-KR");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        int savedCount = 0;

        try {
            Player player = playerRepository.findByName(playerName)
                    .orElseGet(() -> playerRepository.save(Player.builder().name(playerName).build()));

            // --- 1. 로그인 ---
            driver.get("http://www.pingpongking.com/loginForm.asp");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.name("userid")));

            // ▼▼▼▼▼ 아이디 입력 로직을 JavascriptExecutor로 전면 교체 ▼▼▼▼▼
            JavascriptExecutor js = (JavascriptExecutor) driver;
            WebElement userIdInput = driver.findElement(By.name("userid"));
            js.executeScript("arguments[0].focus();", userIdInput);
            js.executeScript("arguments[0].value = '';", userIdInput);
            js.executeScript("arguments[0].value = arguments[1];", userIdInput, accountProperties.getPingpongking().getUsername());
            js.executeScript("arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", userIdInput);
            log.info("[{}] Javascript로 아이디 입력 완료.", getSiteName());
            // ▲▲▲▲▲ 여기까지 수정 ▲▲▲▲▲

            js.executeScript("document.getElementsByName('pwd')[0].value = arguments[0];", accountProperties.getPingpongking().getPassword());
            log.info("[{}] Javascript로 비밀번호 입력 완료.", getSiteName());

            driver.findElement(By.name("frmLogin")).submit();
            log.info("[{}] 로그인 시도...", getSiteName());

            try {
                // '성공 이미지'가 보이거나 또는 '실패 Alert'가 나타날 때까지 최대 10초 대기
                wait.until(ExpectedConditions.or(
                        ExpectedConditions.presenceOfElementLocated(By.cssSelector("img[src='/Img/Bg/loginRightOn.png']")),
                        ExpectedConditions.alertIsPresent()
                ));

                // 대기가 끝난 후, Alert가 있는지 확인하여 실패 여부를 판단
                try {
                    Alert alert = driver.switchTo().alert();
                    log.warn("[{}] 로그인 실패 Alert 발생: {}", getSiteName(), alert.getText());
                    alert.accept();
                    return; // 크롤링 중단
                } catch (NoAlertPresentException e) {
                    // Alert가 없으면 성공한 것이므로, 정상적으로 로그를 남기고 진행
                    log.info("[{}] 로그인 성공.", getSiteName());
                }

            } catch (TimeoutException e) {
                // 10초 동안 성공도 실패도 감지되지 않으면 타임아웃 처리
                log.error("[{}] 로그인 시간 초과. 성공 또는 실패를 확인할 수 없습니다.", getSiteName());
                return;
            }


            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("img[src='/Img/Bg/loginRightOn.png']")));
            log.info("[{}] 로그인 성공.", getSiteName());

            // --- 2. 새 창 열기 및 제어권 전환 ---
            String originalWindow = driver.getWindowHandle();
            js.executeScript("fnOpenSearchPlayer()");
            log.info("[{}] 'fnOpenSearchPlayer()' Javascript 함수 실행 완료.", getSiteName());
            wait.until(ExpectedConditions.numberOfWindowsToBe(2));

            for (String windowHandle : driver.getWindowHandles()) {
                if (!originalWindow.equals(windowHandle)) {
                    driver.switchTo().window(windowHandle);
                    log.info("[{}] 새로 열린 검색창으로 제어권 전환 성공.", getSiteName());
                    break;
                }
            }

            // --- 3. 선수 검색 ---
            WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("idMemberName")));
            searchInput.clear();
            searchInput.sendKeys(playerName);
            log.info("[{}] 검색어 '{}' 입력 완료", getSiteName(), playerName);

            js.executeScript("fnSearchMember()");
            log.info("[{}] 검색 버튼(Javascript) 클릭 완료", getSiteName());

            // --- 4. 결과 테이블 로딩 대기 ---
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table.csTableBase")));
            log.info("[{}] 검색 결과 테이블 로딩 확인", getSiteName());

            // --- 5. 결과 파싱 ---
            Document doc = Jsoup.parse(driver.getPageSource());
            Elements rows = doc.select("table.csTableBase tr");
            List<String> prizedPlacings = List.of("1위", "2위", "3위");

            for (Element row : rows) {
                if (row.select("th").size() > 0) continue;
                Elements cells = row.select("td");
                if (cells.size() < 7) continue;

                try {
                    String placing = cells.get(6).text().trim();
                    if (placing.isEmpty() || prizedPlacings.stream().noneMatch(placing::contains)) {
                        continue;
                    }

                    String dateStr = cells.get(0).text().trim();
                    String tournamentName = cells.get(1).text().trim();
                    String division = cells.get(2).text().trim();
                    String detail = cells.get(4).text().trim();

                    LocalDate tournamentDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yy.MM.dd"));

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
                } catch (Exception e) {
                    log.error("[{}] 행 처리 중 오류 발생: {}", getSiteName(), row.html(), e);
                }
            }
        } catch (Exception e) {
            log.error("[{}] 크롤링 중 최종 오류 발생: {}", getSiteName(), e.getMessage(), e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
        log.info("[{}] 크롤링 완료: {}개의 새로운 기록 저장됨", getSiteName(), savedCount);
    }
}