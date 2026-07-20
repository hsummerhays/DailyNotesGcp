package com.hsummerhays.cloudnotes.note.application;

import com.hsummerhays.cloudnotes.note.domain.Note;
import com.hsummerhays.cloudnotes.note.domain.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoteServiceTest {

    private NoteRepository noteRepository;
    private NoteService noteService;

    @BeforeEach
    void setUp() {
        noteRepository = mock(NoteRepository.class);
        noteService = new NoteService(noteRepository);
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createNote_savesAndReturnsNoteOwnedByCaller() {
        Note note = noteService.createNote("Title", "Content", "owner@example.com");

        assertThat(note.getTitle()).isEqualTo("Title");
        assertThat(note.getOwnerEmail()).isEqualTo("owner@example.com");
        assertThat(note.isArchived()).isFalse();
        verify(noteRepository).save(note);
    }

    @Test
    void getNote_ownedByCaller_returnsNote() {
        UUID id = UUID.randomUUID();
        Note note = new Note(id, "Title", "Content", "owner@example.com");
        when(noteRepository.findById(id)).thenReturn(Optional.of(note));

        assertThat(noteService.getNote(id, "owner@example.com")).isSameAs(note);
    }

    @Test
    void getNote_ownedBySomeoneElse_throwsAccessDenied() {
        UUID id = UUID.randomUUID();
        Note note = new Note(id, "Title", "Content", "owner@example.com");
        when(noteRepository.findById(id)).thenReturn(Optional.of(note));

        assertThatThrownBy(() -> noteService.getNote(id, "attacker@example.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getNote_unknownId_throwsAccessDenied() {
        UUID id = UUID.randomUUID();
        when(noteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getNote(id, "owner@example.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateNote_ownedBySomeoneElse_throwsAndDoesNotSave() {
        UUID id = UUID.randomUUID();
        Note note = new Note(id, "Title", "Content", "owner@example.com");
        when(noteRepository.findById(id)).thenReturn(Optional.of(note));

        assertThatThrownBy(() -> noteService.updateNote(id, "New", "New", "attacker@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(note.getTitle()).isEqualTo("Title");
    }

    @Test
    void archiveNote_marksArchivedAndPersists() {
        UUID id = UUID.randomUUID();
        Note note = new Note(id, "Title", "Content", "owner@example.com");
        when(noteRepository.findById(id)).thenReturn(Optional.of(note));

        Note archived = noteService.archiveNote(id, "owner@example.com");

        assertThat(archived.isArchived()).isTrue();
        verify(noteRepository).save(note);
    }

    @Test
    void restoreNote_clearsArchivedFlag() {
        UUID id = UUID.randomUUID();
        Note note = new Note(id, "Title", "Content", "owner@example.com");
        note.archive();
        when(noteRepository.findById(id)).thenReturn(Optional.of(note));

        Note restored = noteService.restoreNote(id, "owner@example.com");

        assertThat(restored.isArchived()).isFalse();
    }

    @Test
    void deleteNote_ownedBySomeoneElse_throwsAndDoesNotDelete() {
        UUID id = UUID.randomUUID();
        Note note = new Note(id, "Title", "Content", "owner@example.com");
        when(noteRepository.findById(id)).thenReturn(Optional.of(note));

        assertThatThrownBy(() -> noteService.deleteNote(id, "attacker@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(noteRepository, never()).deleteById(any());
    }

    @Test
    void getActiveNotes_blankQuery_listsAllActiveInsteadOfSearching() {
        noteService.getActiveNotes("   ", "owner@example.com");

        verify(noteRepository).findAllActive("owner@example.com");
        verify(noteRepository, never()).search(any(), any());
    }

    @Test
    void getActiveNotes_withQuery_delegatesToSearch() {
        noteService.getActiveNotes("keyword", "owner@example.com");

        verify(noteRepository).search("keyword", "owner@example.com");
    }
}
