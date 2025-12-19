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
                        .requestMatchers("/", "/index.html", "/manifest.json", "/*.png", "/*.jpg", "/favicon.ico").permitAll()
                        .requestMatchers("/api/**").permitAll()  // Alle API-Endpunkte öffentlich
                        .requestMatchers("/admin/**").authenticated()  // Nur Admin-Bereich geschützt
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> {});
        return http.build();
    }
}

        return http.build();
    }
}
