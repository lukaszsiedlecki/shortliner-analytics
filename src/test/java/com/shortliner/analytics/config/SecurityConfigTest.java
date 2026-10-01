package com.shortliner.analytics.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void mapsKeycloakRealmRolesToSpringRoles() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u1")
                .claim("realm_access", Map.of("roles", List.of("user", "admin")))
                .build();

        assertThat(config.jwtAuthenticationConverter().convert(jwt).getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_user", "ROLE_admin");
    }

    @Test
    void tokenWithoutRealmRolesHasNoAuthorities() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u1").build();

        assertThat(config.jwtAuthenticationConverter().convert(jwt).getAuthorities()).isEmpty();
    }
}
