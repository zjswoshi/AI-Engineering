package com.example.starter.controller;

import com.example.starter.exception.ApiError;
import com.example.starter.exception.ApiException;
import com.example.starter.service.IdentityService;
import com.example.starter.service.IdentityService.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class IdentityController {
    private final IdentityService identity;

    IdentityController(IdentityService identity) {
        this.identity = identity;
    }

    @GetMapping("/api/me")
    Profile me(Authentication authentication) {
        return identity.profile(authentication.getName(), authentication.getAuthorities());
    }

    @GetMapping("/api/admin/analyst")
    Profile analyst() {
        return identity.analyst();
    }

    @PutMapping(path = "/api/admin/analyst", consumes = MediaType.APPLICATION_JSON_VALUE)
    Profile changeAnalystRole(@RequestBody RoleChange change, Authentication authentication) {
        if (!"VIEWER".equals(change.role()) && !"OPERATOR".equals(change.role())) {
            throw new ApiException(ApiError.INVALID_ROLE);
        }
        return identity.changeAnalystRole(change.role(), authentication.getName());
    }

    record RoleChange(String role) {}
}
