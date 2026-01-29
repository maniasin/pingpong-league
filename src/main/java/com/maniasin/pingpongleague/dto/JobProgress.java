package com.maniasin.pingpongleague.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class JobProgress {
    private String status;      // IN_PROGRESS, COMPLETED, FAILED
    private int percentage;     // 0 ~ 100
    private String message;     // "에어핑 크롤링 중..."
}