package com.maniasin.pingpongleague.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FinalResultDto {
    private String winner;
    private String runnerUp;
    private List<String> jointThird;
    private List<String> rankings;
}
