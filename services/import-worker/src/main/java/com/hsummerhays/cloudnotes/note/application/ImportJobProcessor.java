package com.hsummerhays.cloudnotes.note.application;

import com.hsummerhays.cloudnotes.note.domain.ImportJob;
import com.hsummerhays.cloudnotes.note.domain.ImportJobItem;
import com.hsummerhays.cloudnotes.note.domain.ImportJobItemRepository;
import com.hsummerhays.cloudnotes.note.domain.ImportJobRepository;
import com.hsummerhays.cloudnotes.note.domain.NoteDocument;
import com.hsummerhays.cloudnotes.note.infrastructure.MongoNoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Kept as its own bean (rather than a method on ImportMessageConsumer) so that
 * {@link #processJob} is invoked through the Spring-managed proxy and its
 * {@code @Transactional} boundary actually applies - calling it as a plain "this."
 * method from within the same class would silently bypass the proxy.
 */
@Service
public class ImportJobProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImportJobProcessor.class);
    private static final int BATCH_SIZE = 1000;

    private final ImportJobRepository importJobRepository;
    private final ImportJobItemRepository importJobItemRepository;
    private final MongoNoteRepository mongoNoteRepository;

    public ImportJobProcessor(
            ImportJobRepository importJobRepository,
            ImportJobItemRepository importJobItemRepository,
            MongoNoteRepository mongoNoteRepository
    ) {
        this.importJobRepository = importJobRepository;
        this.importJobItemRepository = importJobItemRepository;
        this.mongoNoteRepository = mongoNoteRepository;
    }

    @Transactional
    public void processJob(UUID jobId) {
        // Atomically flip PENDING -> PROCESSING so that a redelivered or concurrently-handled
        // copy of this message (e.g. multiple worker replicas) can't both win the job and
        // double-process it; only the caller that actually claims a row proceeds.
        if (importJobRepository.claimForProcessing(jobId) == 0) {
            log.warn("Job {} was not claimed (missing, or not in PENDING status); skipping", jobId);
            return;
        }

        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Claimed job vanished: " + jobId));
        log.info("Processing job: {}, total items: {}", jobId, job.getTotalItems());

        List<ImportJobItem> items = importJobItemRepository.findByJobId(jobId);
        int processedCount = 0;
        int failedCount = 0;

        for (int start = 0; start < items.size(); start += BATCH_SIZE) {
            List<ImportJobItem> chunk = items.subList(start, Math.min(start + BATCH_SIZE, items.size()));

            // Note IDs are derived from the job item's own ID rather than a fresh random UUID,
            // so re-running a chunk (after a crash/redelivery) upserts the same Mongo document
            // instead of inserting a duplicate note.
            List<NoteDocument> notes = chunk.stream()
                    .map(item -> new NoteDocument(item.getId(), item.getTitle(), item.getContent(), job.getUserId()))
                    .toList();

            try {
                mongoNoteRepository.saveAll(notes);
                chunk.forEach(item -> item.setStatus("COMPLETED"));
                processedCount += chunk.size();
            } catch (Exception e) {
                log.error("Failed to import note batch [{}, {}) for job {}", start, start + chunk.size(), jobId, e);
                chunk.forEach(item -> {
                    item.setStatus("FAILED");
                    item.setErrorMessage(e.getMessage());
                });
                failedCount += chunk.size();
            }
            importJobItemRepository.saveAll(chunk);
        }

        job.setProcessedItems(processedCount);
        job.setFailedItems(failedCount);
        job.setStatus(failedCount == 0 ? "COMPLETED" : "FAILED");
        job.setCompletedAt(LocalDateTime.now());
        importJobRepository.save(job);
        log.info("Finished processing job: {} with status: {}", jobId, job.getStatus());
    }
}
