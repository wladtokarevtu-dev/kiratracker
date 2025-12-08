package de.wlad.kiratracker;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 1. Alles Statische + PWA Files erlauben
                .requestMatchers("/", "/index.html", "/manifest.json", "/sw.js", "/favicon.ico", "/apple-touch-icon.png").permitAll()
                
                // 2. ÖFFENTLICHE API (Lesen & Eintragen für alle erlaubt)
                .requestMatchers(HttpMethod.GET, "/status", "/leaderboard").permitAll()
                .requestMatchers(HttpMethod.POST, "/walk", "/walk/request", "/walk/*/applause").permitAll()

                // 3. ADMIN API (Nur mit Passwort / Schloss-Icon)
                .requestMatchers("/admin/**").authenticated()
                
                // Alles andere sicherheitshalber sperren
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .formLogin(Customizer.withDefaults());

        return http.build();
    }
}