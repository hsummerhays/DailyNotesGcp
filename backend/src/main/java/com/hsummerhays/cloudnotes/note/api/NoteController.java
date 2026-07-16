package com.hsummerhays.cloudnotes.note.api;

import com.hsummerhays.cloudnotes.note.application.BulkImportService;
import com.hsummerhays.cloudnotes.note.application.NoteService;
import com.hsummerhays.cloudnotes.note.domain.Note;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;
    private final BulkImportService bulkImportService;

    public NoteController(NoteService noteService, BulkImportService bulkImportService) {
        this.noteService = noteService;
        this.bulkImportService = bulkImportService;
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('ROLE_USER')")
    public ImportTaskStatus importNotes(@Valid @RequestBody BulkImportRequest request, Principal principal) {
        return bulkImportService.startImport(request.notes(), principal.getName());
    }

    @GetMapping("/import/{taskId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ImportTaskStatus getImportStatus(@PathVariable("taskId") UUID taskId, Principal principal) {
        return bulkImportService.getStatus(taskId, principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ROLE_USER')")
    public NoteResponse createNote(@Valid @RequestBody CreateNoteRequest request, Principal principal) {
        Note note = noteService.createNote(request.title(), request.content(), principal.getName());
        return NoteResponse.from(note);
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_USER')")
    public List<NoteResponse> getActiveNotes(
            @RequestParam(value = "query", required = false) String query,
            Principal principal
    ) {
        return noteService.getActiveNotes(query, principal.getName()).stream()
                .map(NoteResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/archived")
    @PreAuthorize("hasRole('ROLE_USER')")
    public List<NoteResponse> getArchivedNotes(Principal principal) {
        return noteService.getArchivedNotes(principal.getName()).stream()
                .map(NoteResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public NoteResponse getNote(@PathVariable("id") UUID id, Principal principal) {
        Note note = noteService.getNote(id, principal.getName());
        return NoteResponse.from(note);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public NoteResponse updateNote(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateNoteRequest request,
            Principal principal
    ) {
        Note note = noteService.updateNote(id, request.title(), request.content(), principal.getName());
        return NoteResponse.from(note);
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('ROLE_USER')")
    public NoteResponse archiveNote(@PathVariable("id") UUID id, Principal principal) {
        Note note = noteService.archiveNote(id, principal.getName());
        return NoteResponse.from(note);
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ROLE_USER')")
    public NoteResponse restoreNote(@PathVariable("id") UUID id, Principal principal) {
        Note note = noteService.restoreNote(id, principal.getName());
        return NoteResponse.from(note);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ROLE_USER')")
    public void deleteNote(@PathVariable("id") UUID id, Principal principal) {
        noteService.deleteNote(id, principal.getName());
    }
}
