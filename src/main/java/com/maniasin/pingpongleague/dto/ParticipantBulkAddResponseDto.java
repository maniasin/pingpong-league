package com.maniasin.pingpongleague.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ParticipantBulkAddResponseDto {
    private List<String> added;
    private List<ParticipantAddFailureDto> failed;
}
