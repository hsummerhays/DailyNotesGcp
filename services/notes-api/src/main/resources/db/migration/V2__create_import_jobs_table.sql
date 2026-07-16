CREATE TABLE import_job (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    source_file_name VARCHAR(255),
    total_items INT NOT NULL,
    processed_items INT NOT NULL,
    failed_items INT NOT NULL,
    idempotency_key VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT
);

CREATE TABLE import_job_item (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES import_job(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT
);
