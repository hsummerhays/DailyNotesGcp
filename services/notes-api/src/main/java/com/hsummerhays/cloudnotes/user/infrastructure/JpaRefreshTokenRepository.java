package com.hsummerhays.cloudnotes.user.infrastructure;

import com.hsummerhays.cloudnotes.user.domain.RefreshToken;
import com.hsummerhays.cloudnotes.user.domain.RefreshTokenRepository;
import com.hsummerhays.cloudnotes.user.domain.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaRefreshTokenRepository implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository repository;

    public JpaRefreshTokenRepository(SpringDataRefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return repository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return repository.findByToken(token);
    }

    @Override
    public void deleteByUser(User user) {
        repository.deleteByUser(user);
    }

    @Override
    public void deleteByToken(String token) {
        repository.deleteByToken(token);
    }
}
