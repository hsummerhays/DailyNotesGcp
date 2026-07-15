package com.hsummerhays.cloudnotes.note.application;

import com.hsummerhays.cloudnotes.note.domain.Note;
import com.hsummerhays.cloudnotes.note.domain.NoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NoteService {

    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public Note createNote(String title, String content, String ownerEmail) {
        Note note = new Note(UUID.randomUUID(), title, content, ownerEmail);
        return noteRepository.save(note);
    }

    @Transactional(readOnly = true)
    public List<Note> getActiveNotes(String query, String ownerEmail) {
        if (query != null && !query.isBlank()) {
            return noteRepository.search(query, ownerEmail);
        }
        return noteRepository.findAllActive(ownerEmail);
    }

    @Transactional(readOnly = true)
    public List<Note> getArchivedNotes(String ownerEmail) {
        return noteRepository.findAllArchived(ownerEmail);
    }

    @Transactional(readOnly = true)
    public Note getNote(UUID id, String ownerEmail) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Note not found with id: " + id));
        if (!note.getOwnerEmail().equals(ownerEmail)) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to view this note");
        }
        return note;
    }

    public Note updateNote(UUID id, String title, String content, String ownerEmail) {
        Note note = getNote(id, ownerEmail);
        note.update(title, content);
        return noteRepository.save(note);
    }

    public Note archiveNote(UUID id, String ownerEmail) {
        Note note = getNote(id, ownerEmail);
        note.archive();
        return noteRepository.save(note);
    }

    public Note restoreNote(UUID id, String ownerEmail) {
        Note note = getNote(id, ownerEmail);
        note.restore();
        return noteRepository.save(note);
    }

    public void deleteNote(UUID id, String ownerEmail) {
        Note note = getNote(id, ownerEmail);
        noteRepository.deleteById(note.getId());
    }
}
