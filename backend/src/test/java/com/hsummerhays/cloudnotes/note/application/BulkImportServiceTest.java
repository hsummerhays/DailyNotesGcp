package com.hsummerhays.cloudnotes.note.application;

import com.hsummerhays.cloudnotes.note.api.CreateNoteRequest;
import com.hsummerhays.cloudnotes.note.api.ImportTaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BulkImportServiceTest {

    private NoteService noteService;
    private BulkImportService bulkImportService;

    @BeforeEach
    void setUp() {
        noteService = mock(NoteService.class);
        bulkImportService = new BulkImportService(noteService);
    }

    // NOTE: @Async only takes effect when Spring proxies the bean. Calling the
    // plain `new BulkImportService(...)` instance directly here means
    // processImportAsync runs synchronously, so assertions can run right after
    // startImport() returns with no polling/waiting required.

    @Test
    void startImport_createsNoteForEachRequestAndMarksCompleted() {
        List<CreateNoteRequest> requests = List.of(
                new CreateNoteRequest("A", "a"),
                new CreateNoteRequest("B", "b")
        );

        ImportTaskStatus initial = bulkImportService.startImport(requests, "owner@example.com");
        ImportTaskStatus finalStatus = bulkImportService.getStatus(initial.taskId(), "owner@example.com");

        assertThat(finalStatus.status()).isEqualTo("COMPLETED");
        assertThat(finalStatus.processedCount()).isEqualTo(2);
        verify(noteService).createNote("A", "a", "owner@example.com");
        verify(noteService).createNote("B", "b", "owner@example.com");
    }

    @Test
    void startImport_whenNoteCreationFails_marksTaskFailed() {
        when(noteService.createNote(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("db down"));

        ImportTaskStatus initial = bulkImportService.startImport(
                List.of(new CreateNoteRequest("A", "a")), "owner@example.com");
        ImportTaskStatus finalStatus = bulkImportService.getStatus(initial.taskId(), "owner@example.com");

        assertThat(finalStatus.status()).isEqualTo("FAILED");
    }

    @Test
    void getStatus_unknownTaskId_throwsIllegalArgument() {
        assertThatThrownBy(() -> bulkImportService.getStatus(UUID.randomUUID(), "owner@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getStatus_requestedByDifferentUser_throwsAccessDenied() {
        ImportTaskStatus initial = bulkImportService.startImport(
                List.of(new CreateNoteRequest("A", "a")), "owner@example.com");

        assertThatThrownBy(() -> bulkImportService.getStatus(initial.taskId(), "attacker@example.com"))
                .isInstanceOf(AccessDeniedException.class);
    }
}
