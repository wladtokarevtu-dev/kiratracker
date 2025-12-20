package de.wlad.kiratracker;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Statische Dateien und Root
                        .requestMatchers("/", "/index.html", "/manifest.json", "/favicon.ico").permitAll()
                        .requestMatchers("/*.png", "/*.jpg", "/*.ico", "/*.css", "/*.js").permitAll()

                        // Alle API Endpunkte öffentlich
                        .requestMatchers("/api/**").permitAll()

                        // Health Check
                        .requestMatchers("/health").permitAll()

                        // Status Endpoints
                        .requestMatchers("/status", "/leaderboard", "/walk/**", "/food/**").permitAll()

                        // Nur Admin geschützt
                        .requestMatchers("/admin", "/admin/**").authenticated()

                        // Rest erlaubt (für Development)
                        .anyRequest().permitAll()
                )
                .httpBasic(basic -> {});

        return http.build();
    }
}