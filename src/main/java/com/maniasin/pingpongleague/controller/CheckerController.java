package com.maniasin.pingpongleague.controller;

import com.maniasin.pingpongleague.domain.AwardRecord;
import com.maniasin.pingpongleague.dto.JobProgress;
import com.maniasin.pingpongleague.service.CheckerService;
import com.maniasin.pingpongleague.service.CrawlerService;
import com.maniasin.pingpongleague.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/checker")
@RequiredArgsConstructor
public class CheckerController {

    private final CheckerService checkerService;
    private final CrawlerService crawlerService;
    private final RateLimitService rateLimitService;

    @GetMapping
    public String checkerHomePage() {
        return "checker-home";
    }

    @PostMapping("/search")
    @ResponseBody
    public ResponseEntity<?> startSearch(
            @RequestParam String playerName,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String username = userDetails != null ? userDetails.getUsername() : "anonymous";
        
        // Rate Limiting 체크
        if (!rateLimitService.allowRequest(username)) {
            int remaining = rateLimitService.getRemainingRequests(username);
            log.warn("Rate limit 초과: 사용자 '{}', 선수명 '{}'", username, playerName);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error", "요청 횟수 제한 초과",
                            "message", "분당 조회 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.",
                            "remainingRequests", remaining
                    ));
        }
        
        log.info("크롤링 요청: 사용자 '{}', 선수명 '{}'", username, playerName);
        String jobId = crawlerService.startCrawlingJob(playerName);
        int remaining = rateLimitService.getRemainingRequests(username);
        
        Map<String, Object> response = Map.of(
                "jobId", jobId,
                "playerName", playerName,
                "remainingRequests", remaining
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @ResponseBody
    public ResponseEntity<JobProgress> checkStatus(@RequestParam String jobId) {
        JobProgress progress = crawlerService.getJobStatus(jobId);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/results")
    public String showResults(@RequestParam String playerName, Model model) {
        List<AwardRecord> records = checkerService.getExistingRecords(playerName);

        // 집계 로직
        Map<String, Long> placingSummary = records.stream()
                .map(record -> record.getPlacing().trim())
                .collect(Collectors.groupingBy(
                        placing -> {
                            if (placing.contains("준우승") || placing.contains("2위")) return "준우승";
                            if (placing.contains("우승") || placing.contains("1위")) return "우승"; // "우승"은 두 번째로 체크
                            if (placing.contains("3위") || placing.contains("4강")) return "3위";
                            return "기타";
                        },
                        Collectors.counting()
                ));

        model.addAttribute("playerName", playerName);
        model.addAttribute("records", records);
        model.addAttribute("placingSummary", placingSummary);

        return "checker-results";
    }
}