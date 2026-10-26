package com.studentissue.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentissue.management.dto.IssueCreateRequest;
import com.studentissue.management.dto.ResolutionConfirmRequest;
import com.studentissue.management.dto.VoteRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IssueRestControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET / should return 200 OK and render Thymeleaf index template")
    void shouldRenderIndexPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("issues"))
                .andExpect(model().attributeExists("stats"))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    @DisplayName("REST API: Create issue, list, vote, and confirm resolution")
    void testCompleteRestFlow() throws Exception {
        String browserId = UUID.randomUUID().toString();

        // 1. Create Issue via POST /api/issues
        IssueCreateRequest createRequest = new IssueCreateRequest(
                "Projector not turning on in Seminar Hall 1",
                "HDMI cable is cut and power button is unresponsive.",
                "Academic",
                "Seminar Hall 1"
        );

        String createResponseJson = mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.status", is("OPEN")))
                .andExpect(jsonPath("$.data.priority", is("LOW")))
                .andReturn().getResponse().getContentAsString();

        Number issueIdNumber = com.jayway.jsonpath.JsonPath.read(createResponseJson, "$.data.id");
        Long issueId = issueIdNumber.longValue();

        // 2. Upvote Issue via POST /api/issues/{id}/vote
        VoteRequest voteRequest = new VoteRequest(browserId);
        mockMvc.perform(post("/api/issues/" + issueId + "/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.voteCount", is(1)));

        // 3. Duplicate vote from same browser identifier should return 409 Conflict
        mockMvc.perform(post("/api/issues/" + issueId + "/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));

        // 4. Confirm resolution via POST /api/issues/{id}/confirm-resolution
        ResolutionConfirmRequest confirmRequest = new ResolutionConfirmRequest(browserId);
        mockMvc.perform(post("/api/issues/" + issueId + "/confirm-resolution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.confirmationCount", is(1)))
                .andExpect(jsonPath("$.data.status", is("OPEN")));

        // 5. Four more distinct browser confirmations to trigger automatic RESOLVED status
        for (int i = 2; i <= 5; i++) {
            ResolutionConfirmRequest nextBrowserConfirm = new ResolutionConfirmRequest(UUID.randomUUID().toString());
            mockMvc.perform(post("/api/issues/" + issueId + "/confirm-resolution")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(nextBrowserConfirm)))
                    .andExpect(status().isOk());
        }

        // 6. Verify status changed to RESOLVED
        mockMvc.perform(get("/api/issues/" + issueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RESOLVED")))
                .andExpect(jsonPath("$.data.confirmationCount", is(5)));
    }

    @Test
    @DisplayName("REST API: GET /api/issues/search?keyword=... should find issues by title, description, category, or location")
    void testSearchIssuesEndpoint() throws Exception {
        // Create an issue to search for
        IssueCreateRequest createRequest = new IssueCreateRequest(
                "Broken Water Cooler in Science Block",
                "Water cooler compressor is leaking freon on ground floor.",
                "Hostel & Mess",
                "Science Block Ground Floor"
        );
        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // 1. Search by title keyword
        mockMvc.perform(get("/api/issues/search").param("keyword", "Cooler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].title", containsString("Water Cooler")));

        // 2. Search by description keyword
        mockMvc.perform(get("/api/issues/search").param("keyword", "freon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        // 3. Search by location keyword
        mockMvc.perform(get("/api/issues/search").param("keyword", "Science Block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        // 4. Empty keyword should return all issues without error
        mockMvc.perform(get("/api/issues/search").param("keyword", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()));

        // 5. Search with category filter
        mockMvc.perform(get("/api/issues/search")
                        .param("keyword", "Cooler")
                        .param("category", "Hostel & Mess"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        // 6. Non-matching search keyword
        mockMvc.perform(get("/api/issues/search").param("keyword", "NonExistentKeywordXYZ123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("REST API: client errors return 400/404/409 with a JSON body instead of 500")
    void testErrorResponses() throws Exception {
        String validBody = objectMapper.writeValueAsString(new VoteRequest(UUID.randomUUID().toString()));

        // Unknown issue -> 404
        mockMvc.perform(get("/api/issues/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(post("/api/issues/999999/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));

        // Non-numeric id -> 400
        mockMvc.perform(post("/api/issues/abc/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));

        // Invalid UUID -> 400
        mockMvc.perform(post("/api/issues/1/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"browserId\":\"not-a-uuid\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));

        // Broken JSON -> 400
        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));

        // Missing required fields -> 400 with field errors
        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.data.title", notNullValue()));

        // Unknown category -> 400
        IssueCreateRequest badCategory = new IssueCreateRequest("Title", "Description", "Unknown", "Block A");
        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badCategory)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("REST API: confirming an already RESOLVED issue returns 400")
    void testConfirmAfterResolved() throws Exception {
        IssueCreateRequest createRequest = new IssueCreateRequest(
                "Broken bench", "Bench near canteen is broken", "Campus Facilities", "Canteen");
        String json = mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(json, "$.data.id");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/issues/" + id + "/confirm-resolution")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ResolutionConfirmRequest(UUID.randomUUID().toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.confirmationsRequired", is(5)));
        }

        mockMvc.perform(post("/api/issues/" + id + "/confirm-resolution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResolutionConfirmRequest(UUID.randomUUID().toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("REST API: GET /api/issues with category and status filter")
    void testFilterIssues() throws Exception {
        mockMvc.perform(get("/api/issues")
                        .param("category", "Infrastructure")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()));
    }
}
