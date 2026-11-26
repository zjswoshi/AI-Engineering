package com.example.starter.config;

import com.example.starter.repository.PostgresAccountRepository;
import com.example.starter.controller.BladeAccountsController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("blade & postgres")
class BladeAccountsConfig {
    @Bean
    ApplicationRunner bladeAdmin(PostgresAccountRepository accounts, PasswordEncoder encoder,
                                @Value("${BLADE_ADMIN_PASSWORD:}") String password) {
        return arguments -> {
            if (!BladeAccountsController.validPassword(password)) {
                throw new IllegalArgumentException("BLADE_ADMIN_PASSWORD requires at least 12 characters and at most 72 UTF-8 bytes");
            }
            accounts.createAccountIfAbsent("admin", encoder.encode(password), "ADMIN");
        };
    }
}
