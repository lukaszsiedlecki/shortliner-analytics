package com.shortliner.analytics.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();

    // Spring Security 7 also adds a FACTOR_BEARER authority for every bearer token; only realm roles matter here
    private List<String> roles(Jwt jwt) {
        return config.jwtAuthenticationConverter().convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();
    }

    @Test
    void mapsKeycloakRealmRolesToSpringRoles() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u1")
                .claim("realm_access", Map.of("roles", List.of("user", "admin")))
                .build();

        assertThat(roles(jwt)).containsExactlyInAnyOrder("ROLE_user", "ROLE_admin");
    }

    @Test
    void tokenWithoutRealmRolesHasNoRoles() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u1").build();

        assertThat(roles(jwt)).isEmpty();
    }
}
