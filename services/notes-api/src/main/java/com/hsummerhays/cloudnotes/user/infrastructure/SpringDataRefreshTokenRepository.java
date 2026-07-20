package com.hsummerhays.cloudnotes.user.infrastructure;

import com.hsummerhays.cloudnotes.user.domain.RefreshToken;
import com.hsummerhays.cloudnotes.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
    void deleteByToken(String token);
}
