package com.hsummerhays.cloudnotes.note.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoteRepository {
    Note save(Note note);
    Optional<Note> findById(UUID id);
    List<Note> findAllActive(String ownerEmail);
    List<Note> findAllArchived(String ownerEmail);
    List<Note> search(String query, String ownerEmail);
    void deleteById(UUID id);
}
