package com.hsummerhays.cloudnotes.user.api;

import com.hsummerhays.cloudnotes.security.JwtService;
import com.hsummerhays.cloudnotes.security.SecurityUser;
import com.hsummerhays.cloudnotes.user.application.RefreshTokenService;
import com.hsummerhays.cloudnotes.user.application.UserService;
import com.hsummerhays.cloudnotes.user.domain.RefreshToken;
import com.hsummerhays.cloudnotes.user.domain.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String COOKIE_NAME = "access_token";
    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Value("${app.jwt.cookie-secure}")
    private boolean cookieSecure;

    @Value("${app.jwt.cookie-same-site}")
    private String cookieSameSite;

    public AuthController(UserService userService, RefreshTokenService refreshTokenService, JwtService jwtService) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        userService.registerUser(request.email(), request.password(), request.displayName());
        UserService.AuthenticationResult result = userService.authenticate(request.email(), request.password());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(result.user());

        setAuthCookie(response, result.token());
        setRefreshCookie(response, refreshToken.getToken());

        return new AuthResponse(result.user().getEmail(), result.user().getDisplayName());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        // Do not look up the user separately before authenticating: that would let an unknown
        // email fail on the lookup (404) while a known email with a wrong password fails on
        // authenticate() (401), letting an attacker enumerate registered emails by status code.
        UserService.AuthenticationResult result = userService.authenticate(request.email(), request.password());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(result.user());

        setAuthCookie(response, result.token());
        setRefreshCookie(response, refreshToken.getToken());

        return new AuthResponse(result.user().getEmail(), result.user().getDisplayName());
    }

    @PostMapping("/logout")
    public void logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        clearAuthCookie(response);
        clearRefreshCookie(response);
        if (refreshToken != null) {
            refreshTokenService.revokeByToken(refreshToken);
        }
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME) String refreshToken,
            HttpServletResponse response
    ) {
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken);
        User user = newRefreshToken.getUser();
        String accessToken = jwtService.generateToken(new SecurityUser(user));

        setAuthCookie(response, accessToken);
        setRefreshCookie(response, newRefreshToken.getToken());

        return new AuthResponse(user.getEmail(), user.getDisplayName());
    }

    @GetMapping("/me")
    public AuthResponse me(Principal principal) {
        User user = userService.getUserByEmail(principal.getName());
        return new AuthResponse(user.getEmail(), user.getDisplayName());
    }

    private void setAuthCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ofMillis(jwtExpirationMs))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ofMillis(refreshExpirationMs))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearAuthCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
