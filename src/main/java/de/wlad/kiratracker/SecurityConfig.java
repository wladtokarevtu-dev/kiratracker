package de.wlad.kiratracker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${APP_SECURITY_USERNAME}")
    private String adminUser;

    @Value("${APP_SECURITY_PASSWORD}")
    private String adminPassword;

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

    @Bean
    public UserDetailsService users() {
        if (adminUser == null || adminUser.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException("APP_SECURITY_USERNAME und APP_SECURITY_PASSWORD müssen gesetzt sein");
        }

        var user = User.withUsername(adminUser)
                .password(adminPassword)
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
        // Für dein kleines Projekt: Passwort im Klartext aus ENV vergleichen.
        // Später kannst du hier auf BCrypt wechseln.
        return NoOpPasswordEncoder.getInstance();
    }
}
