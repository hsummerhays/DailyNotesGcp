package com.hsummerhays.cloudnotes.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "dGhpcy1pcy1hLXZlcnktc2VjdXJlLTMyLWJ5dGUtc2VjcmV0LWtleS1mb3ItdGVzdGluZw==");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);
    }

    private UserDetails userNamed(String email) {
        return User.withUsername(email).password("hash").authorities("ROLE_USER").build();
    }

    @Test
    void generateToken_thenExtractUsername_roundTrips() {
        UserDetails userDetails = userNamed("user@example.com");

        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_tokenIssuedForDifferentUser_returnsFalse() {
        String token = jwtService.generateToken(userNamed("owner@example.com"));

        assertThat(jwtService.isTokenValid(token, userNamed("attacker@example.com"))).isFalse();
    }

    @Test
    void extractUsername_expiredToken_throws() {
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", -1000L);
        String token = jwtService.generateToken(userNamed("user@example.com"));

        // jjwt validates the exp claim during parsing itself, before any of
        // JwtService's own expiry checks run - JwtAuthenticationFilter relies on
        // this by wrapping extractUsername in a try/catch.
        assertThatThrownBy(() -> jwtService.extractUsername(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
