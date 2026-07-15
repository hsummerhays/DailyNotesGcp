package com.hsummerhays.cloudnotes.note.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "notes")
public class NoteDocument {

    @Id
    private UUID id;

    @Field("title")
    private String title;

    @Field("content")
    private String content;

    @Field("archived")
    private boolean archived;

    @Field("owner_email")
    private String ownerEmail;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;

    @Field("version")
    private Long version;

    public NoteDocument() {
    }

    public NoteDocument(UUID id, String title, String content, String ownerEmail) {
        this.id = id != null ? id : UUID.randomUUID();
        this.title = title;
        this.content = content;
        this.ownerEmail = ownerEmail;
        this.archived = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.version = 0L;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
        this.updatedAt = Instant.now();
        this.version = this.version != null ? this.version + 1 : 1L;
    }

    public void archive() {
        this.archived = true;
        this.updatedAt = Instant.now();
    }

    public void restore() {
        this.archived = false;
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isArchived() { return archived; }
    public String getOwnerEmail() { return ownerEmail; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
