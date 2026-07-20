package com.hsummerhays.cloudnotes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsummerhays.cloudnotes.note.domain.Note;
import com.hsummerhays.cloudnotes.note.domain.NoteRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.hsummerhays.cloudnotes.security.RateLimitingFilter;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/**
 * End-to-end tests through the real HTTP + Spring Security stack (cookie auth,
 * CORS-adjacent config, ownership checks, validation, exception handling).
 * Postgres/users runs against the H2 "test" profile; MongoDB/notes is replaced
 * with an in-memory fake below so these tests need no external services.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NoteFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @MockitoBean
    private NoteRepository noteRepository;

    @MockitoBean
    private com.google.cloud.spring.pubsub.core.PubSubTemplate pubSubTemplate;

    private final Map<UUID, Note> store = new ConcurrentHashMap<>();

    @BeforeEach
    void fakeNoteRepository() {
        rateLimitingFilter.reset();
        org.mockito.Mockito.when(pubSubTemplate.publish(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture("msg-id"));
        store.clear();
        when(noteRepository.save(any())).thenAnswer(inv -> {
            Note note = inv.getArgument(0);
            store.put(note.getId(), note);
            return note;
        });
        when(noteRepository.findById(any())).thenAnswer(inv ->
                Optional.ofNullable(store.get((UUID) inv.getArgument(0))));
        when(noteRepository.findAllActive(any())).thenAnswer(inv -> store.values().stream()
                .filter(n -> !n.isArchived() && n.getOwnerEmail().equals(inv.getArgument(0)))
                .collect(Collectors.toList()));
        when(noteRepository.findAllArchived(any())).thenAnswer(inv -> store.values().stream()
                .filter(n -> n.isArchived() && n.getOwnerEmail().equals(inv.getArgument(0)))
                .collect(Collectors.toList()));
        doAnswer(inv -> store.remove((UUID) inv.getArgument(0)))
                .when(noteRepository).deleteById(any());
    }

    private Cookie registerAndGetAuthCookie(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\",\"displayName\":\"Test User\"}"
                                .formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("access_token");
        assertThat(cookie).as("auth cookie should be set on register").isNotNull();
        return cookie;
    }

    @Test
    void contextLoads() {
    }

    @Test
    void registerCreateAndFetchNote_happyPath() throws Exception {
        Cookie authCookie = registerAndGetAuthCookie("owner@example.com");

        mockMvc.perform(post("/api/notes")
                        .cookie(authCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hello\",\"content\":\"World\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Hello"));

        mockMvc.perform(get("/api/notes").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void accessingAnotherUsersNote_isForbidden() throws Exception {
        Cookie ownerCookie = registerAndGetAuthCookie("owner2@example.com");
        Cookie attackerCookie = registerAndGetAuthCookie("attacker2@example.com");

        String body = mockMvc.perform(post("/api/notes")
                        .cookie(ownerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Secret\",\"content\":\"Private\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String noteId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/notes/" + noteId).cookie(attackerCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestWithoutAuthCookie_isRejected() throws Exception {
        mockMvc.perform(get("/api/notes"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void registeringSameEmailTwice_isRejected() throws Exception {
        registerAndGetAuthCookie("duplicate@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"duplicate@example.com\",\"password\":\"password123\",\"displayName\":\"Dup\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void bulkImport_acceptsJobAndOnlyOwnerCanReadStatus() throws Exception {
        // Actual note creation now happens out-of-process in the import-worker service after
        // it consumes the Pub/Sub notification this endpoint publishes, so here we only assert
        // that the job is durably queued as PENDING and ownership is enforced on the status read.
        Cookie ownerCookie = registerAndGetAuthCookie("importer@example.com");
        Cookie otherCookie = registerAndGetAuthCookie("nosy@example.com");

        String body = mockMvc.perform(post("/api/notes/import")
                        .cookie(ownerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":[{\"title\":\"A\",\"content\":\"a\"},{\"title\":\"B\",\"content\":\"b\"}]}"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        String taskId = json.get("taskId").asText();

        mockMvc.perform(get("/api/notes/import/" + taskId).cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalCount").value(2));

        mockMvc.perform(get("/api/notes/import/" + taskId).cookie(otherCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void logout_clearsSessionSoSubsequentRequestsAreRejected() throws Exception {
        Cookie authCookie = registerAndGetAuthCookie("logout@example.com");

        mockMvc.perform(get("/api/auth/me").cookie(authCookie))
                .andExpect(status().isOk());

        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .cookie(authCookie)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();
        Cookie clearedCookie = logoutResult.getResponse().getCookie("access_token");

        mockMvc.perform(get("/api/auth/me").cookie(clearedCookie))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void refreshSession_rotatesTokensSuccessfully() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"refreshable@example.com\",\"password\":\"password123\",\"displayName\":\"Test User\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie refreshCookie = result.getResponse().getCookie("refresh_token");
        assertThat(refreshCookie).as("refresh token cookie should be set").isNotNull();

        // Perform token rotation
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        Cookie newAuthCookie = refreshResult.getResponse().getCookie("access_token");
        Cookie newRefreshCookie = refreshResult.getResponse().getCookie("refresh_token");

        assertThat(newAuthCookie).as("new auth cookie should be returned").isNotNull();
        assertThat(newRefreshCookie).as("new refresh cookie should be returned").isNotNull();
        assertThat(newRefreshCookie.getValue()).isNotEqualTo(refreshCookie.getValue());
    }

    @Test
    void rateLimiting_blocksExcessiveRequests() throws Exception {
        // We have configured capacity=5 in test properties.
        // Consume all tokens
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/notes"))
                    .andExpect(status().is4xxClientError());
        }

        // The 6th request should be rate-limited
        mockMvc.perform(get("/api/notes"))
                .andExpect(status().isTooManyRequests());
    }
}
