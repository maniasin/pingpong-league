package com.maniasin.pingpongleague.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화

                // H2 Console이 프레임 안에서 동작하도록 설정
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.disable())
                )

                .authorizeHttpRequests(authorize -> authorize
                        // H2 Console 접속을 위한 경로와 다른 정적 리소스 경로를 모두 허용
                        .requestMatchers("/", "/api/users/signup", "/login", "/signup", "/css/**", "/js/**", "/h2-console/**").permitAll()
                        .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout") // 로그아웃을 처리할 URL
                        .logoutSuccessUrl("/login") // 로그아웃 성공 시 이동할 URL
                        .invalidateHttpSession(true) // 세션 무효화
                        .deleteCookies("JSESSIONID") // JSESSIONID 쿠키 삭제
                        .permitAll()
                )

                .userDetailsService(userDetailsService);

        return http.build();
    }
}