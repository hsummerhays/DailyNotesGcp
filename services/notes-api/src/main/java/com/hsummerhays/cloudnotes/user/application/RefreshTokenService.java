package com.hsummerhays.cloudnotes.user.application;

import com.hsummerhays.cloudnotes.user.domain.RefreshToken;
import com.hsummerhays.cloudnotes.user.domain.RefreshTokenRepository;
import com.hsummerhays.cloudnotes.user.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-ms:604800000}") // Default 7 days
    private long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public RefreshToken createRefreshToken(User user) {
        // Revoke existing tokens for the user to prevent session bloat
        refreshTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusMillis(refreshExpirationMs);
        RefreshToken refreshToken = new RefreshToken(UUID.randomUUID(), token, user, expiryDate);

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.deleteByToken(token.getToken());
            throw new InvalidRefreshTokenException("Refresh token was expired. Please make a new signin request");
        }
        if (token.isRevoked()) {
            throw new InvalidRefreshTokenException("Refresh token has been revoked");
        }
        return token;
    }

    public RefreshToken rotateRefreshToken(String tokenString) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        verifyExpiration(token);

        User user = token.getUser();
        // Delete the old token
        refreshTokenRepository.deleteByToken(tokenString);

        // Create a new one
        return createRefreshToken(user);
    }

    public void revokeByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    public void revokeByToken(String tokenString) {
        refreshTokenRepository.deleteByToken(tokenString);
    }
}
