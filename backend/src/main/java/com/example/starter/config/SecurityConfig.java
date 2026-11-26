package com.example.starter.config;

import com.example.starter.exception.ApiError;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfig {
    private final JacksonJsonHttpMessageConverter problemJson = new JacksonJsonHttpMessageConverter();

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, @Value("${starter.auth.basic-challenge:true}") boolean basicChallenge) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, failure) -> {
            if (basicChallenge && !"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Realm\"");
            }
            writeProblem(response, ApiError.AUTHENTICATION_REQUIRED);
        };
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(routes -> routes
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/detections", "/api/detection-tasks")
                        .hasAnyRole("OPERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/detection-tasks", "/api/detection-tasks/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/me", "/api/detections").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler((request, response, failure) -> writeProblem(response, ApiError.ACCESS_DENIED)))
                .httpBasic(basic -> basic.authenticationEntryPoint(unauthorized))
                .build();
    }

    private void writeProblem(HttpServletResponse response, ApiError error) throws IOException {
        var problem = error.problem();
        response.setStatus(problem.getStatus());
        var output = new ServletServerHttpResponse(response);
        problemJson.write(problem, MediaType.APPLICATION_PROBLEM_JSON, output);
    }
}
