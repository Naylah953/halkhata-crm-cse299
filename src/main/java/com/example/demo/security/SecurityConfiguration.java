package com.example.demo.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Hook into your CorsConfig file so the Bouncer knows the global rules
                .cors(Customizer.withDefaults())

                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req ->
                        req
                                // Let the invisible browser Preflight (OPTIONS) requests pass without a JWT!
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                // --- THE ONLY NEW ADDITION: Allow the browser to load the frontend files ---
                                .requestMatchers("/", "/crm.html", "/assets/**", "/css/**", "/js/**").permitAll()

                                // Anyone can login and register
                                .requestMatchers("/api/auth/**", "/error").permitAll()

                                // ONLY Admins can hire new staff (POST)
                                .requestMatchers(HttpMethod.POST, "/api/users/staff").hasRole("ADMIN")

                                // ONLY Admins can update the business info (PUT)
                                .requestMatchers(HttpMethod.PUT, "/api/tenant/me").hasRole("ADMIN")

                                // THE NEW RULE: ONLY Admins can delete staff (DELETE)
                                .requestMatchers(HttpMethod.DELETE, "/api/users/staff/**").hasRole("ADMIN")

                                // For EVERYTHING else (like GET /api/users/staff and GET /api/tenant/me),
                                // you just need to be a logged-in user!
                                .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}