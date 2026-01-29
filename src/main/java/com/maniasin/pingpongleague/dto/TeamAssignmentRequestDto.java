package com.maniasin.pingpongleague.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class TeamAssignmentRequestDto {
    private List<TeamCreateRequestDto> teams;
}
