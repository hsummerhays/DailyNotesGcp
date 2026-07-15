package com.hsummerhays.cloudnotes;

import com.hsummerhays.cloudnotes.note.domain.Note;
import com.hsummerhays.cloudnotes.note.domain.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class NoteFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NoteRepository noteRepository;

    @BeforeEach
    public void setup() {
        // In local development integration testing without a running DB container,
        // we can run a H2/in-memory DB or Mock/Stub if needed.
        // For simplicity under Phase 1, we write a standard SpringBootTest.
    }

    @Test
    public void contextLoads() {
        // Verifies Spring Context starts successfully
    }
}
