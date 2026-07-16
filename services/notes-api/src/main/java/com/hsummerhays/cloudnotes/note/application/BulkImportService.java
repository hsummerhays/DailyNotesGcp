package com.hsummerhays.cloudnotes.note.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.hsummerhays.cloudnotes.note.api.CreateNoteRequest;
import com.hsummerhays.cloudnotes.note.api.ImportTaskStatus;
import com.hsummerhays.cloudnotes.note.domain.ImportJob;
import com.hsummerhays.cloudnotes.note.domain.ImportJobItem;
import com.hsummerhays.cloudnotes.note.domain.ImportJobItemRepository;
import com.hsummerhays.cloudnotes.note.domain.ImportJobRepository;
import com.hsummerhays.cloudnotes.note.domain.ImportRequestedEvent;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BulkImportService {

    private final ImportJobRepository importJobRepository;
    private final ImportJobItemRepository importJobItemRepository;
    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    public BulkImportService(
            ImportJobRepository importJobRepository,
            ImportJobItemRepository importJobItemRepository,
            PubSubTemplate pubSubTemplate,
            ObjectMapper objectMapper
    ) {
        this.importJobRepository = importJobRepository;
        this.importJobItemRepository = importJobItemRepository;
        this.pubSubTemplate = pubSubTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImportTaskStatus startImport(List<CreateNoteRequest> requests, String ownerEmail) {
        UUID jobId = UUID.randomUUID();

        // 1. Create and persist the parent Job state
        ImportJob job = new ImportJob(jobId, ownerEmail, requests.size(), null);
        importJobRepository.save(job);

        // 2. Persist the job items for the worker to process, in one batch insert
        List<ImportJobItem> items = requests.stream()
                .map(req -> new ImportJobItem(UUID.randomUUID(), job, req.title(), req.content()))
                .toList();
        importJobItemRepository.saveAll(items);

        // 3. Publish a lightweight notification event over Pub/Sub
        publishImportRequested(jobId, ownerEmail);

        return new ImportTaskStatus(jobId, requests.size(), 0, "PENDING");
    }

    private void publishImportRequested(UUID jobId, String ownerEmail) {
        try {
            String messageJson = objectMapper.writeValueAsString(new ImportRequestedEvent(jobId, ownerEmail));
            pubSubTemplate.publish("notes-import-topic", messageJson);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize import event for job " + jobId, e);
        }
    }

    public ImportTaskStatus getStatus(UUID jobId, String ownerEmail) {
        ImportJob job = importJobRepository.findByIdAndUserId(jobId, ownerEmail)
                .orElseThrow(() -> new AccessDeniedException("You do not have permission to view this import task or it does not exist"));
        
        return new ImportTaskStatus(
                job.getId(),
                job.getTotalItems(),
                job.getProcessedItems(),
                job.getStatus()
        );
    }
}
