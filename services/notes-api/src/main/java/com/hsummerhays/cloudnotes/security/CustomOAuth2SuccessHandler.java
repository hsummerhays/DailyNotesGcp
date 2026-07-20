package com.hsummerhays.cloudnotes.security;

import com.hsummerhays.cloudnotes.user.application.UserService;
import com.hsummerhays.cloudnotes.user.domain.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.hsummerhays.cloudnotes.user.application.RefreshTokenService;
import com.hsummerhays.cloudnotes.user.domain.RefreshToken;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Component
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

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

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    public CustomOAuth2SuccessHandler(
            @org.springframework.context.annotation.Lazy UserService userService,
            @org.springframework.context.annotation.Lazy RefreshTokenService refreshTokenService,
            JwtService jwtService
    ) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            String name = oidcUser.getFullName();
            if (name == null || name.isBlank()) {
                name = oidcUser.getGivenName();
            }
            if (name == null || name.isBlank()) {
                name = email;
            }

            User user;
            try {
                user = userService.getUserByEmail(email);
            } catch (IllegalArgumentException e) {
                // Register the user with a random secure password hash
                user = userService.registerUser(email, UUID.randomUUID().toString(), name);
            }

            SecurityUser securityUser = new SecurityUser(user);
            String token = jwtService.generateToken(securityUser);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite(cookieSameSite)
                    .path("/")
                    .maxAge(Duration.ofMillis(jwtExpirationMs))
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken.getToken())
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite(cookieSameSite)
                    .path("/")
                    .maxAge(Duration.ofMillis(refreshExpirationMs))
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            getRedirectStrategy().sendRedirect(request, response, redirectUri);
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }
}
