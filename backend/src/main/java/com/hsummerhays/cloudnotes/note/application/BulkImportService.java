package com.hsummerhays.cloudnotes.note.application;

import com.hsummerhays.cloudnotes.note.api.CreateNoteRequest;
import com.hsummerhays.cloudnotes.note.api.ImportTaskStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BulkImportService {

    private final NoteService noteService;
    private final Map<UUID, ImportTaskStatus> taskRegistry = new ConcurrentHashMap<>();
    private final Map<UUID, String> taskOwners = new ConcurrentHashMap<>();

    public BulkImportService(NoteService noteService) {
        this.noteService = noteService;
    }

    public ImportTaskStatus startImport(List<CreateNoteRequest> requests, String ownerEmail) {
        UUID taskId = UUID.randomUUID();
        taskOwners.put(taskId, ownerEmail);
        ImportTaskStatus initialStatus = new ImportTaskStatus(taskId, requests.size(), 0, "PENDING");
        taskRegistry.put(taskId, initialStatus);

        // Trigger async execution
        processImportAsync(taskId, requests, ownerEmail);

        return initialStatus;
    }

    @Async
    public void processImportAsync(UUID taskId, List<CreateNoteRequest> requests, String ownerEmail) {
        taskRegistry.put(taskId, new ImportTaskStatus(taskId, requests.size(), 0, "PROCESSING"));
        
        int processed = 0;
        for (CreateNoteRequest request : requests) {
            try {
                noteService.createNote(request.title(), request.content(), ownerEmail);
                processed++;
                
                // Keep status updated
                taskRegistry.put(taskId, new ImportTaskStatus(taskId, requests.size(), processed, "PROCESSING"));
            } catch (Exception e) {
                taskRegistry.put(taskId, new ImportTaskStatus(taskId, requests.size(), processed, "FAILED"));
                return;
            }
        }
        
        taskRegistry.put(taskId, new ImportTaskStatus(taskId, requests.size(), processed, "COMPLETED"));
    }

    public ImportTaskStatus getStatus(UUID taskId, String ownerEmail) {
        String owner = taskOwners.get(taskId);
        if (owner == null) {
            throw new IllegalArgumentException("Import task not found with id: " + taskId);
        }
        if (!owner.equals(ownerEmail)) {
            throw new AccessDeniedException("You do not have permission to view this import task");
        }
        return taskRegistry.get(taskId);
    }
}
