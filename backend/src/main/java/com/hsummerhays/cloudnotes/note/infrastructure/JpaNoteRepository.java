package com.hsummerhays.cloudnotes.note.infrastructure;

import com.hsummerhays.cloudnotes.note.domain.Note;
import com.hsummerhays.cloudnotes.note.domain.NoteRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaNoteRepository implements NoteRepository {

    private final SpringDataNoteRepository repository;

    public JpaNoteRepository(SpringDataNoteRepository repository) {
        this.repository = repository;
    }

    @Override
    public Note save(Note note) {
        return repository.save(note);
    }

    @Override
    public Optional<Note> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<Note> findAllActive(String ownerEmail) {
        return repository.findAllActive(ownerEmail);
    }

    @Override
    public List<Note> findAllArchived(String ownerEmail) {
        return repository.findAllArchived(ownerEmail);
    }

    @Override
    public List<Note> search(String query, String ownerEmail) {
        if (query == null || query.isBlank()) {
            return repository.findAllActive(ownerEmail);
        }
        return repository.searchActive(query, ownerEmail);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
