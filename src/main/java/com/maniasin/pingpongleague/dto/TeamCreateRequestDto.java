package com.maniasin.pingpongleague.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class TeamCreateRequestDto {
    private String name;
    private List<Long> memberIds;
}
