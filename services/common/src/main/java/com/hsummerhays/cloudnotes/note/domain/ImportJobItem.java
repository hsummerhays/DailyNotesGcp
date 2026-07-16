package com.hsummerhays.cloudnotes.note.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "import_job_item")
public class ImportJobItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private ImportJob job;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String status; // PENDING, COMPLETED, FAILED

    @Column(name = "error_message")
    private String errorMessage;

    public ImportJobItem() {}

    public ImportJobItem(UUID id, ImportJob job, String title, String content) {
        this.id = id;
        this.job = job;
        this.title = title;
        this.content = content;
        this.status = "PENDING";
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ImportJob getJob() { return job; }
    public void setJob(ImportJob job) { this.job = job; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
