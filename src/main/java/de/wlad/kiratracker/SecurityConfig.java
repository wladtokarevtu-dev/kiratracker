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
                        // Statische Dateien
                        .requestMatchers("/", "/index.html", "/manifest.json", "/favicon.ico", "/*.png", "/*.jpg", "/*.ico").permitAll()
                        // Alle API Endpunkte öffentlich
                        .requestMatchers("/api/**").permitAll()
                        // Health Check
                        .requestMatchers("/health").permitAll()
                        // Nur Admin geschützt
                        .requestMatchers("/admin/**").authenticated()
                        // Rest braucht Auth
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> {});
        return http.build();
    }
}
