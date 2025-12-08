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
                // 1. CSRF deaktivieren, damit POST-Requests (Speichern) einfach funktionieren
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // 2. Öffentliche Bereiche definieren (kein Login nötig)
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/status").permitAll()      // Status abrufen
                        .requestMatchers(HttpMethod.POST, "/walk").permitAll()       // Spaziergang eintragen
                        .requestMatchers(HttpMethod.POST, "/walk/request").permitAll() // Anfrage stellen
                        .requestMatchers(HttpMethod.POST, "/walk/**/applause").permitAll() // Applaus geben

                        // 3. Admin-Bereiche schützen (Login nötig)
                        .requestMatchers("/walk/request/**/approve").authenticated()
                        .requestMatchers("/walk/request/**/reject").authenticated()

                        // Alles andere erfordert ebenfalls Login
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}

