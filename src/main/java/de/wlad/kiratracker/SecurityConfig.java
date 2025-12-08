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
                .csrf(csrf -> csrf.disable()) // Wichtig für POST Requests
                .authorizeHttpRequests(auth -> auth
                        // 1. Statische Ressourcen IMMER erlauben
                        .requestMatchers("/", "/index.html", "/admin.html", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                        // 2. Öffentliche API Endpoints
                        .requestMatchers(HttpMethod.GET, "/status").permitAll()
                        .requestMatchers(HttpMethod.POST, "/walk").permitAll()
                        .requestMatchers(HttpMethod.POST, "/walk/request").permitAll()
                        .requestMatchers(HttpMethod.POST, "/walk/*/applause").permitAll()

                        // 3. Admin API Endpoints schützen (Muss eingeloggt sein)
                        .requestMatchers("/admin/**").authenticated() // Schützt alle API Calls unter /admin/...

                        // Alles andere erfordert Login
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
