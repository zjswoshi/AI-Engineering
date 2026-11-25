package com.example.starter.repository;

import org.springframework.security.core.userdetails.UserDetailsService;

public interface AccountRepository extends UserDetailsService {
    String replaceRole(String username, String role);
}
