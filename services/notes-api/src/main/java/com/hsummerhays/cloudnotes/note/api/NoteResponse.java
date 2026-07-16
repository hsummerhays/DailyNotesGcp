package com.hsummerhays.cloudnotes.note.api;

import com.hsummerhays.cloudnotes.note.domain.Note;
import java.time.Instant;
import java.util.UUID;

public record NoteResponse(
    UUID id,
    String title,
    String content,
    boolean archived,
    Instant createdAt,
    Instant updatedAt,
    Long version
) {
    public static NoteResponse from(Note note) {
        return new NoteResponse(
            note.getId(),
            note.getTitle(),
            note.getContent(),
            note.isArchived(),
            note.getCreatedAt(),
            note.getUpdatedAt(),
            note.getVersion()
        );
    }
}
