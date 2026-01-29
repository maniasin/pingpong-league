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
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class IpingCrawler implements SiteCrawler {

    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;
    private final AwardRecordRepository awardRecordRepository;
    private final AccountProperties accountProperties;

    @Override
    public String getSiteName() {
        return "아이핑";
    }

    @Override
    public void scrape(String playerName) {
        WebDriverManager.chromedriver().setup();
        log.info("[{}] 크롤링 시작: {}", getSiteName(), playerName);

        Player player = playerRepository.findByName(playerName)
                .orElseGet(() -> playerRepository.save(Player.builder().name(playerName).build()));

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
        options.addArguments("--window-size=1920,1080");


        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        int savedCount = 0;

        try {
            driver.get("http://www.iping.club/?pg=login");
            log.info("[{}] 로그인 페이지 접속", getSiteName());

            wait.until(ExpectedConditions.presenceOfElementLocated(By.name("Mid")));
            driver.findElement(By.name("Mid")).sendKeys(accountProperties.getIping().getUsername());
            driver.findElement(By.name("Pwd")).sendKeys(accountProperties.getIping().getPassword());

            WebElement loginButton = driver.findElement(By.cssSelector("input[type='submit'][value='로그인']"));
            loginButton.click();
            log.info("[{}] 로그인 버튼 클릭", getSiteName());

            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(@href, 'logout')]")));
            log.info("[{}] 로그인 성공", getSiteName());

            driver.get("http://www.iping.club/?pg=Search&c=1");
            log.info("[{}] 선수 검색 페이지로 이동", getSiteName());

            wait.until(ExpectedConditions.presenceOfElementLocated(By.name("SchVal")));
            WebElement searchInput = driver.findElement(By.name("SchVal"));
            searchInput.clear();
            searchInput.sendKeys(playerName);
            log.info("[{}] 검색어 '{}' 입력", getSiteName(), playerName);

            WebElement searchButton = driver.findElement(By.cssSelector("input[type='submit'][name='sch']"));
            searchButton.click();
            log.info("[{}] 검색 버튼 클릭", getSiteName());


            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//b[text()='입상이력']")));
            log.info("[{}] 결과 페이지 로딩 확인", getSiteName());

            Document doc = Jsoup.parse(driver.getPageSource());
            Document docToSearch;

            Element innerHtmlContainer = doc.selectFirst("td:has(html)");
            if (innerHtmlContainer != null) {
                log.info("[{}] 중첩된 HTML 구조를 발견하여 내부 문서를 파싱합니다.", getSiteName());
                docToSearch = Jsoup.parse(innerHtmlContainer.html());
            } else {
                log.info("[{}] 중첩된 HTML 구조가 없어 기본 문서에서 검색합니다.", getSiteName());
                docToSearch = doc;
            }

            Element resultTable = docToSearch.selectFirst("table.text14:has(td:contains(입상이력))");
            if (resultTable == null) {
                log.info("[{}] 입상 이력 테이블을 찾을 수 없습니다.", getSiteName());
                return;
            }

            Elements awardRows = resultTable.select("tr[style='background:#ffffff;']");
            log.info("[{}] 검색 결과 행 {}건 발견", getSiteName(), awardRows.size());

            for (Element row : awardRows) {
                Elements cells = row.select("td");

                if (cells.size() != 2) {
                    log.info("[{}] 입상 기록 행이 아니므로 건너뜁니다: {}", getSiteName(), row.text());
                    continue;
                }

                Element block = cells.get(1);

                try {
                    String tournamentName = Optional.ofNullable(block.selectFirst("b:contains(회)"))
                            .map(Element::text).orElse("").trim();

                    String dateText = Optional.ofNullable(block.selectFirst("span.text14"))
                            .map(Element::text).orElse("");

                    Pattern datePattern = Pattern.compile("(\\d{4}년 \\d{2}월 \\d{2}일)");
                    Matcher matcher = datePattern.matcher(dateText);
                    if (!matcher.find()) {
                        log.warn("[{}] 날짜 패턴이 맞지 않아 건너뜁니다. 날짜 텍스트: '{}'", getSiteName(), dateText);
                        continue;
                    }
                    String dateStr = matcher.group(1);
                    log.debug("[{}] 정규식으로 추출한 날짜: '{}'", getSiteName(), dateStr);

                    // ▼▼▼ [수정] 부서/성적 링크를 정확히 선택하도록 Selector 수정 ▼▼▼
                    Element linkElement = block.selectFirst("span.btn_white_gray > a[href*='/?pg=CVR']");
                    String division = "";
                    String placing = "";

                    if (linkElement != null) {
                        Element placingElement = linkElement.selectFirst("b:contains(우승), b:contains(준우승), b:contains(3위), img[src*='rr2.png']");

                        if (placingElement != null) {
                            placing = placingElement.tagName().equals("img") ? "준우승" : placingElement.text().trim();

                            String fullLinkText = linkElement.text();
                            division = fullLinkText.replace(placing, "").trim();
                        } else {
                            division = linkElement.text().trim();
                        }
                    }

                    if (tournamentName.isEmpty() || dateStr.isEmpty() || placing.isEmpty() || division.isEmpty()) {
                        log.warn("[{}] 필수 정보(대회명, 날짜, 성적, 부서) 중 누락된 항목이 있어 건너뜁니다. 대회명: '{}', 날짜: '{}', 성적: '{}', 부서: '{}'", getSiteName(), tournamentName, dateStr, placing, division);
                        continue;
                    }

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
                    LocalDate tournamentDate = LocalDate.parse(dateStr, formatter);

                    Tournament tournament = tournamentRepository.findByNameAndTournamentDate(tournamentName, tournamentDate)
                            .orElseGet(() -> {
                                log.info("[{}] 새 대회 정보를 저장합니다: {}", getSiteName(), tournamentName);
                                return tournamentRepository.save(Tournament.builder()
                                        .name(tournamentName).tournamentDate(tournamentDate).organizer(getSiteName()).build());
                            });

                    if (!awardRecordRepository.existsByPlayerIdAndTournamentIdAndDivisionAndPlacing(
                            player.getId(), tournament.getId(), division, placing)) {

                        AwardRecord record = AwardRecord.builder()
                                .player(player).tournament(tournament).division(division).detail("").placing(placing)
                                .build();
                        awardRecordRepository.save(record);
                        savedCount++;
                        log.info("[{}] 신규 기록 저장: {} | {} | {} | {}", getSiteName(), tournamentName, division, placing, player.getName());
                    } else {
                        log.info("[{}] 이미 존재하는 기록입니다: {} | {} | {}", getSiteName(), tournamentName, division, placing);
                    }
                } catch (Exception e) {
                    log.error("[{}] 결과 블록 처리 중 예측하지 못한 오류 발생: {}", getSiteName(), block.html(), e);
                }
            }
        } catch (Exception e) {
            log.error("[{}] 크롤링 프로세스 중 심각한 오류 발생", getSiteName(), e);
        } finally {
            driver.quit();
        }
        log.info("[{}] 크롤링 완료: {}개의 새로운 기록 저장", getSiteName(), savedCount);
    }
}
