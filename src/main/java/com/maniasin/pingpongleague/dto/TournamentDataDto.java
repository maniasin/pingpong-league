package com.maniasin.pingpongleague.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentDataDto {
    private Map<String, Object> standardData;
    private Map<String, Object> splitData;
}
