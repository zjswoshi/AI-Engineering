package com.example.starter.service;

import com.example.starter.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class IdentityService {
    private static final Logger LOG = LoggerFactory.getLogger(IdentityService.class);

    private final AccountRepository users;

    IdentityService(AccountRepository users) {
        this.users = users;
    }

    public Profile profile(String username, Iterable<? extends GrantedAuthority> authorities) {
        String role = authorities.iterator().next().getAuthority().substring("ROLE_".length());
        return new Profile(username, role);
    }

    public Profile analyst() {
        UserDetails analyst = users.loadUserByUsername("analyst");
        return profile(analyst.getUsername(), analyst.getAuthorities());
    }

    public Profile changeAnalystRole(String role, String actor) {
        String previousRole = users.replaceRole("analyst", role);
        Profile updated = new Profile("analyst", role);
        LOG.atInfo()
                .addKeyValue("actor", actor)
                .addKeyValue("target", updated.username())
                .addKeyValue("previousRole", previousRole)
                .addKeyValue("role", updated.role())
                .log("identity.role_updated");
        return updated;
    }

    public record Profile(String username, String role) {}
}
