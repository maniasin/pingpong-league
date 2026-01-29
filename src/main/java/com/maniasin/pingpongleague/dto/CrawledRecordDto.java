package com.maniasin.pingpongleague.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CrawledRecordDto {
    private String tournamentName;
    private LocalDate tournamentDate;
    private String division;
    private String placing;
}