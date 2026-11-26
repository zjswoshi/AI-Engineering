package com.example.starter.controller;

import com.example.starter.exception.ApiError;
import com.example.starter.exception.ApiException;
import com.example.starter.repository.PostgresAccountRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("blade & postgres")
@RequestMapping("/api/admin/blade-operators")
public class BladeAccountsController {
    private final PostgresAccountRepository accounts;
    private final PasswordEncoder passwords;

    public BladeAccountsController(PostgresAccountRepository accounts, PasswordEncoder passwords) {
        this.accounts = accounts;
        this.passwords = passwords;
    }

    @PostMapping
    ResponseEntity<Void> create(@RequestBody Account input, @RequestHeader(name = "X-Requested-With", defaultValue = "") String header) {
        if (!"XMLHttpRequest".equals(header)) throw new ApiException(ApiError.ACCESS_DENIED);
        if (input.username() == null || !input.username().matches("[A-Za-z0-9._-]{1,64}")
                || !validPassword(input.password())) throw new ApiException(ApiError.INVALID_REQUEST);
        if (!accounts.createAccountIfAbsent(input.username(), passwords.encode(input.password()), "OPERATOR")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public static boolean validPassword(String password) {
        return password != null && password.length() >= 12
                && password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72;
    }

    record Account(String username, String password) {}
}
