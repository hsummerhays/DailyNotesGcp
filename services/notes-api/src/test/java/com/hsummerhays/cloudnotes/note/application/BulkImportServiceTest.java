package com.hsummerhays.cloudnotes.note.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.hsummerhays.cloudnotes.note.api.CreateNoteRequest;
import com.hsummerhays.cloudnotes.note.api.ImportTaskStatus;
import com.hsummerhays.cloudnotes.note.domain.ImportJob;
import com.hsummerhays.cloudnotes.note.domain.ImportJobItem;
import com.hsummerhays.cloudnotes.note.domain.ImportJobItemRepository;
import com.hsummerhays.cloudnotes.note.domain.ImportJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BulkImportServiceTest {

    private ImportJobRepository importJobRepository;
    private ImportJobItemRepository importJobItemRepository;
    private PubSubTemplate pubSubTemplate;
    private BulkImportService bulkImportService;

    @BeforeEach
    void setUp() {
        importJobRepository = mock(ImportJobRepository.class);
        importJobItemRepository = mock(ImportJobItemRepository.class);
        pubSubTemplate = mock(PubSubTemplate.class);
        bulkImportService = new BulkImportService(
                importJobRepository, importJobItemRepository, pubSubTemplate, new ObjectMapper());
    }

    @Test
    void startImport_persistsJobAndItemsAndPublishesEvent() {
        List<CreateNoteRequest> requests = List.of(
                new CreateNoteRequest("A", "a"),
                new CreateNoteRequest("B", "b")
        );

        ImportTaskStatus status = bulkImportService.startImport(requests, "owner@example.com");

        assertThat(status.status()).isEqualTo("PENDING");
        assertThat(status.totalCount()).isEqualTo(2);
        assertThat(status.processedCount()).isEqualTo(0);

        verify(importJobRepository).save(any(ImportJob.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ImportJobItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(importJobItemRepository).saveAll(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue()).hasSize(2);
        assertThat(itemsCaptor.getValue()).extracting(ImportJobItem::getTitle).containsExactly("A", "B");

        verify(pubSubTemplate).publish(eq("notes-import-topic"), contains(status.taskId().toString()));
    }

    @Test
    void getStatus_unknownOrForeignJob_throwsAccessDenied() {
        UUID jobId = UUID.randomUUID();
        when(importJobRepository.findByIdAndUserId(jobId, "owner@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bulkImportService.getStatus(jobId, "owner@example.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getStatus_ownedJob_returnsMappedStatus() {
        UUID jobId = UUID.randomUUID();
        ImportJob job = new ImportJob(jobId, "owner@example.com", 5, null);
        job.setProcessedItems(3);
        job.setStatus("PROCESSING");
        when(importJobRepository.findByIdAndUserId(jobId, "owner@example.com"))
                .thenReturn(Optional.of(job));

        ImportTaskStatus status = bulkImportService.getStatus(jobId, "owner@example.com");

        assertThat(status.totalCount()).isEqualTo(5);
        assertThat(status.processedCount()).isEqualTo(3);
        assertThat(status.status()).isEqualTo("PROCESSING");
    }
}
