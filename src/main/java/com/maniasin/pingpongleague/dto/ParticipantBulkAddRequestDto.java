package com.maniasin.pingpongleague.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ParticipantBulkAddRequestDto {
    private List<String> identifiers;
}
