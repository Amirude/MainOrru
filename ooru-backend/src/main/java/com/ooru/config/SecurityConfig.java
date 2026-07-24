package com.ooru.config;

import com.ooru.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // safe here because we use stateless JWT bearer auth, not cookies
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/register", "/api/auth/verify-otp", "/api/auth/login",
                                 "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                .requestMatchers("/api/categories/**").permitAll()
                .requestMatchers("/api/bus-routes/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/api/search/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/shops/register").hasRole("SHOP_OWNER")
                .requestMatchers("/api/shops/mine/**").hasRole("SHOP_OWNER")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/shops/*/menu").hasRole("SHOP_OWNER")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/shops/*/slots").hasRole("SHOP_OWNER")
                .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/shops/*/billing-settings").hasRole("SHOP_OWNER")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/shops/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

     CorsConfiguration config = new CorsConfiguration();

     config.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "https://*.vercel.app"
     ));

     config.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
     ));

     config.setAllowedHeaders(List.of("*"));
     config.setExposedHeaders(List.of("*"));
     config.setAllowCredentials(true);

     UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

     source.registerCorsConfiguration("/**", config);

     return source;
    }
}
