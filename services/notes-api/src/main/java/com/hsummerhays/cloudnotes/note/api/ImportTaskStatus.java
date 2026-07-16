package com.hsummerhays.cloudnotes.note.api;

import java.util.UUID;

public record ImportTaskStatus(
    UUID taskId,
    int totalCount,
    int processedCount,
    String status // PENDING, PROCESSING, COMPLETED, FAILED
) {}
