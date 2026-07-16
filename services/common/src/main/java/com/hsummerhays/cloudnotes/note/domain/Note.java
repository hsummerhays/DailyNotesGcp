package com.hsummerhays.cloudnotes.note.domain;

import java.time.Instant;
import java.util.UUID;

// Plain domain object - persistence is handled by NoteRepository (MongoDB-backed), not JPA.
public class Note {

    private UUID id;
    private String title;
    private String content;
    private boolean archived;
    private String ownerEmail;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

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
