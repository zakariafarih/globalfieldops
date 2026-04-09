package com.globalfieldops.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()

                        // Audit — admin only
                        .requestMatchers("/api/audit-events/**").hasRole("ADMIN")

                        // Non-health actuator — admin only
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // Technician writes — admin only
                        .requestMatchers(HttpMethod.POST, "/api/technicians/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/technicians/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/technicians/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/technicians/**").hasRole("ADMIN")

                        // Work-order writes — dispatcher or admin
                        .requestMatchers(HttpMethod.POST, "/api/work-orders/**").hasAnyRole("DISPATCHER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/work-orders/**").hasAnyRole("DISPATCHER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/work-orders/**").hasAnyRole("DISPATCHER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/work-orders/**").hasAnyRole("DISPATCHER", "ADMIN")

                        // Authenticated reads
                        .requestMatchers(HttpMethod.GET, "/api/technicians/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/work-orders/**").authenticated()

                        // Error endpoint must be accessible for Spring Boot error handling
                        .requestMatchers("/error").permitAll()

                        // Deny everything else
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                ));

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());
        return converter;
    }
}
