package com.hsummerhays.cloudnotes.note.api;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkImportRequest(
    @NotEmpty(message = "Notes list cannot be empty")
    List<CreateNoteRequest> notes
) {}
