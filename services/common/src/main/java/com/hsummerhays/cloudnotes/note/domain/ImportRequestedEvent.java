package com.hsummerhays.cloudnotes.note.domain;

import java.util.UUID;

public record ImportRequestedEvent(String eventType, int eventVersion, UUID jobId, String userId) {

    public ImportRequestedEvent(UUID jobId, String userId) {
        this("notes.import.requested", 1, jobId, userId);
    }
}
