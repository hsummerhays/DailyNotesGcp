package com.hsummerhays.cloudnotes.note.infrastructure;

import com.hsummerhays.cloudnotes.note.domain.Note;
import com.hsummerhays.cloudnotes.note.domain.NoteDocument;
import com.hsummerhays.cloudnotes.note.domain.NoteRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Primary
public class MongoNoteRepositoryBridge implements NoteRepository {

    private final MongoNoteRepository mongoRepository;

    public MongoNoteRepositoryBridge(MongoNoteRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    private Note toDomain(NoteDocument doc) {
        if (doc == null) return null;
        // Map document fields back to relational Note object to maintain API compatibility
        // Clean architecture boundary allows repository implementation to switch under the hood!
        Note note = new Note(doc.getId(), doc.getTitle(), doc.getContent(), doc.getOwnerEmail());
        if (doc.isArchived()) {
            note.archive();
        }
        return note;
    }

    private NoteDocument toDocument(Note note) {
        if (note == null) return null;
        NoteDocument doc = new NoteDocument(note.getId(), note.getTitle(), note.getContent(), note.getOwnerEmail());
        if (note.isArchived()) {
            doc.archive();
        }
        return doc;
    }

    @Override
    public Note save(Note note) {
        NoteDocument doc = toDocument(note);
        NoteDocument saved = mongoRepository.save(doc);
        return toDomain(saved);
    }

    @Override
    public Optional<Note> findById(UUID id) {
        return mongoRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Note> findAllActive(String ownerEmail) {
        return mongoRepository.findByOwnerEmailAndArchivedFalseOrderByUpdatedAtDesc(ownerEmail).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Note> findAllArchived(String ownerEmail) {
        return mongoRepository.findByOwnerEmailAndArchivedTrueOrderByUpdatedAtDesc(ownerEmail).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Note> search(String query, String ownerEmail) {
        if (query == null || query.isBlank()) {
            return findAllActive(ownerEmail);
        }
        return mongoRepository.searchActive(query, ownerEmail).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        mongoRepository.deleteById(id);
    }
}
