package com.hsummerhays.cloudnotes.user.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshToken() {
        // Required by JPA
    }

    public RefreshToken(UUID id, String token, User user, Instant expiryDate) {
        this.id = id != null ? id : UUID.randomUUID();
        this.token = token;
        this.user = user;
        this.expiryDate = expiryDate;
        this.revoked = false;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getToken() { return token; }
    public User getUser() { return user; }
    public Instant getExpiryDate() { return expiryDate; }
    public boolean isRevoked() { return revoked; }
    public Instant getCreatedAt() { return createdAt; }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isExpired() {
        return expiryDate.isBefore(Instant.now());
    }
}
