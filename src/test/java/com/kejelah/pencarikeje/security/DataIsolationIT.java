package com.kejelah.pencarikeje.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kejelah.pencarikeje.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Definition of Done requirement from MVP.md 10:
 *
 * <blockquote>User A cannot read or mutate any resource belonging to User B via
 * any endpoint, including direct id manipulation.</blockquote>
 *
 * <p>Each endpoint that takes an application id is probed with the wrong owner's
 * token.
 */
class DataIsolationIT extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenA;
    private String tokenB;
    private long applicationOfA;
    private long progressOfA;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("delete from application_progress");
        jdbcTemplate.update("delete from applications");
        jdbcTemplate.update("delete from users");

        tokenA = register("alice@example.com");
        tokenB = register("bob@example.com");

        applicationOfA = createApplication(tokenA);
        progressOfA = firstProgressId(tokenA, applicationOfA);
    }

    @Test
    @DisplayName("B cannot read A's application detail")
    void detailIsForbidden() throws Exception {
        mockMvc.perform(get("/api/applications/" + applicationOfA).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("APPLICATION_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("A's application never appears in B's list")
    void listIsScopedToOwner() throws Exception {
        mockMvc.perform(get("/api/applications").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("B cannot update or delete A's application")
    void mutationIsForbidden() throws Exception {
        String body = """
                {"companyName":"Hijacked","roleName":"Owner","dateApplied":"2026-08-01"}
                """;

        mockMvc.perform(put("/api/applications/" + applicationOfA)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/applications/" + applicationOfA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // The row is untouched.
        mockMvc.perform(get("/api/applications/" + applicationOfA).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Acme"));
    }

    @Test
    @DisplayName("B cannot read or mutate A's progress timeline")
    void progressIsForbidden() throws Exception {
        mockMvc.perform(get("/api/applications/" + applicationOfA + "/progress")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        String body = """
                {"statusId":4,"eventDate":"2026-08-05","notes":"injected"}
                """;

        mockMvc.perform(post("/api/applications/" + applicationOfA + "/progress")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/applications/" + applicationOfA + "/progress/" + progressOfA)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/applications/" + applicationOfA + "/progress/" + progressOfA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("B cannot upload to or download from A's resume slot")
    void resumeIsForbidden() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", "%PDF-1.7 test".getBytes());

        mockMvc.perform(multipart("/api/applications/" + applicationOfA + "/resume").file(pdf)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/applications/" + applicationOfA + "/resume")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("B's dashboard counts only B's applications")
    void dashboardIsScopedToOwner() throws Exception {
        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(0))
                .andExpect(jsonPath("$.recentApplications").isEmpty());

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(1));
    }

    @Test
    @DisplayName("protected endpoints reject a missing or garbage token")
    void unauthenticatedAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        mockMvc.perform(get("/api/applications").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers ------------------------------------------------------------

    private String register(String email) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "name", "Test User",
                "email", email,
                "password", "password123",
                "confirmPassword", "password123"));

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long createApplication(String token) throws Exception {
        String body = """
                {"companyName":"Acme","roleName":"Backend Engineer","dateApplied":"2026-08-01"}
                """;

        MvcResult result = mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long firstProgressId(String token, long applicationId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/applications/" + applicationId + "/progress")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode timeline = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(timeline).hasSize(1);
        return timeline.get(0).get("id").asLong();
    }
}
