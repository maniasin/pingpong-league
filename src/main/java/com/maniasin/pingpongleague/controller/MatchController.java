package com.maniasin.pingpongleague.controller;

import com.maniasin.pingpongleague.dto.MatchResultDto;
import com.maniasin.pingpongleague.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    @PostMapping("/{matchId}/result")
    public ResponseEntity<Void> updateResult(@PathVariable Long matchId, @RequestBody Map<String, Integer> scores) {
        Integer player1Score = scores.get("player1Score");
        Integer player2Score = scores.get("player2Score");

        if (player1Score == null || player2Score == null) {
            return ResponseEntity.badRequest().build();
        }

        matchService.updateMatchResult(matchId, player1Score, player2Score);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bulk-result")
    public ResponseEntity<Void> bulkUpdateResults(@RequestBody List<MatchResultDto> results) {
        matchService.bulkUpdateMatchResults(results);
        return ResponseEntity.ok().build();
    }
}