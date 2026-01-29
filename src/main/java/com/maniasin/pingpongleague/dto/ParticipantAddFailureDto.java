package com.maniasin.pingpongleague.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ParticipantAddFailureDto {
    private String identifier;
    private String reason;
}
