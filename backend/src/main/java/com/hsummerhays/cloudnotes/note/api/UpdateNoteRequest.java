package com.hsummerhays.cloudnotes.note.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNoteRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    String title,
    
    String content
) {}
