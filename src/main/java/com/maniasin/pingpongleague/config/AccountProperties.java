package com.maniasin.pingpongleague.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "account")
@Getter
@Setter
public class AccountProperties {

    private Pingpongking pingpongking = new Pingpongking();
    private Iping iping = new Iping(); // 👈 아이핑 계정 클래스 추가

    @Getter
    @Setter
    public static class Pingpongking {
        private String username;
        private String password;
    }

    @Getter
    @Setter
    public static class Iping { // 👈 아이핑 계정 정보를 담을 내부 클래스
        private String username;
        private String password;
    }
}