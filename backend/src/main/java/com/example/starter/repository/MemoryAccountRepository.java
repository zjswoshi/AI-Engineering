package com.example.starter.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
class MemoryAccountRepository extends InMemoryUserDetailsManager implements AccountRepository {
    MemoryAccountRepository(
            PasswordEncoder encoder,
            @Value("${starter.auth.admin-password}") String adminPassword,
            @Value("${starter.auth.analyst-password}") String analystPassword) {
        super(User.withUsername("admin").password(encoder.encode(adminPassword)).roles("ADMIN").build(),
                User.withUsername("analyst").password(encoder.encode(analystPassword)).roles("VIEWER").build());
    }

    @Override
    public synchronized String replaceRole(String username, String role) {
        var account = loadUserByUsername(username);
        String previousRole = account.getAuthorities().iterator().next().getAuthority().substring("ROLE_".length());
        updateUser(User.withUserDetails(account).roles(role).build());
        return previousRole;
    }
}
