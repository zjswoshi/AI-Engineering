package com.example.starter.config;

import com.example.starter.repository.PostgresAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("postgres")
@ConditionalOnProperty(name = "starter.auth.demo-accounts-enabled", havingValue = "true")
class PostgresDemoAccountsConfig {
    @Bean
    ApplicationRunner demoAccounts(
            PostgresAccountRepository accounts,
            PasswordEncoder encoder,
            @Value("${starter.auth.admin-password}") String adminPassword,
            @Value("${starter.auth.analyst-password}") String analystPassword) {
        return arguments -> {
            accounts.createDemoAccountIfAbsent("admin", encoder.encode(adminPassword), "ADMIN");
            accounts.createDemoAccountIfAbsent("analyst", encoder.encode(analystPassword), "VIEWER");
        };
    }
}
