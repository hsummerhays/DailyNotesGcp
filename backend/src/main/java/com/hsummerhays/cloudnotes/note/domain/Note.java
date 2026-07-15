package com.hsummerhays.cloudnotes.note.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notes")
public class Note {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "owner_email", nullable = false)
    private String ownerEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Note() {
        // Required by JPA
    }

    public Note(UUID id, String title, String content, String ownerEmail) {
        this.id = id != null ? id : UUID.randomUUID();
        this.title = title;
        this.content = content;
        this.ownerEmail = ownerEmail;
        this.archived = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
        this.updatedAt = Instant.now();
    }

    public void archive() {
        this.archived = true;
        this.updatedAt = Instant.now();
    }

    public void restore() {
        this.archived = false;
        this.updatedAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isArchived() { return archived; }
    public String getOwnerEmail() { return ownerEmail; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
