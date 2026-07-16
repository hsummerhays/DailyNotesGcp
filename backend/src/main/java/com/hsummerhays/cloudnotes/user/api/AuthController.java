package com.hsummerhays.cloudnotes.user.api;

import com.hsummerhays.cloudnotes.user.application.UserService;
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

    private final UserService userService;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.cookie-secure}")
    private boolean cookieSecure;

    @Value("${app.jwt.cookie-same-site}")
    private String cookieSameSite;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        User user = userService.registerUser(request.email(), request.password(), request.displayName());
        String token = userService.authenticate(request.email(), request.password());
        setAuthCookie(response, token);
        return new AuthResponse(user.getEmail(), user.getDisplayName());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        User user = userService.getUserByEmail(request.email());
        String token = userService.authenticate(request.email(), request.password());
        setAuthCookie(response, token);
        return new AuthResponse(user.getEmail(), user.getDisplayName());
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        clearAuthCookie(response);
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
}
