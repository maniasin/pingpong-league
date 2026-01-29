package com.maniasin.pingpongleague;

import com.maniasin.pingpongleague.config.AccountProperties; // import 추가
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties; // import 추가
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync // ★★★ 비동기 기능 활성화
@EnableConfigurationProperties(AccountProperties.class) // 👈 이 어노테이션을 추가!
@SpringBootApplication
public class PingpongLeagueApplication {

	public static void main(String[] args) {
		SpringApplication.run(PingpongLeagueApplication.class, args);
	}

}