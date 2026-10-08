package com.kamsarobar;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * End-to-end walk through the main features against an in-memory database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void fullCommunityFlow() throws Exception {
        long delhi = cityId("Delhi");
        long mumbai = cityId("Mumbai");

        // Form 1: basic registration
        String seeker = register("Asif Khan", "9000000001", delhi);
        String referrer = register("Bilal Khan", "9000000002", delhi);
        String mumbaiMember = register("Danish Khan", "9000000003", mumbai);
        call(post("/api/auth/register"), null, Map.of("name", "Dup", "mobile", "+91 90000 00001",
                "cityId", delhi, "password", "secret123")).andExpect(status().isConflict());
        call(post("/api/auth/login"), null, Map.of("mobile", "9000000001", "password", "wrong"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/posts")).andExpect(status().isUnauthorized());

        // Form 2: detailed profile (editable)
        call(put("/api/profile/me"), referrer, Map.of(
                "linkedinUrl", "linkedin.com/in/bilal",
                "currentCompany", "Google",
                "position", "Senior Engineer",
                "yearsOfExperience", 6,
                "referralCompanies", List.of("Google", "Microsoft", "google "),
                "expertise", List.of("Java", "System Design")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referralCompanies", contains("Google", "Microsoft")))
                .andExpect(jsonPath("$.linkedinUrl").value("https://linkedin.com/in/bilal"));
        call(put("/api/profile/me"), mumbaiMember, Map.of("referralCompanies", List.of("Google India")))
                .andExpect(status().isOk());

        // Referral search by company (case-insensitive, partial) with optional city filter
        call(get("/api/directory/referrers?company=GOOG"), seeker, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Bilal Khan"))
                .andExpect(jsonPath("$.content[0].mobile").value("919000000002"))
                .andExpect(jsonPath("$.content[0].matched", contains("Google")));
        call(get("/api/directory/referrers?company=google&cityId=" + mumbai), seeker, null)
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Danish Khan"));
        call(get("/api/directory/experts?expertise=design"), seeker, null)
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].matched", contains("System Design")));
        call(get("/api/suggestions/companies?q=goo"), seeker, null)
                .andExpect(jsonPath("$", contains("Google", "Google India")));

        // Posts & comments CRUD with ownership rules
        long postId = id(call(post("/api/posts"), seeker,
                Map.of("category", "JOB_OPENING", "title", "Hiring", "content", "Backend role open"))
                .andExpect(status().isCreated()));
        call(put("/api/posts/" + postId), referrer, Map.of("title", "x", "content", "y"))
                .andExpect(status().isForbidden());
        call(put("/api/posts/" + postId), seeker, Map.of("title", "Hiring now", "content", "Backend role"))
                .andExpect(jsonPath("$.title").value("Hiring now"));
        long commentId = id(call(post("/api/posts/" + postId + "/comments"), referrer, Map.of("content", "I can refer"))
                .andExpect(status().isCreated()));
        call(get("/api/posts?cityId=" + delhi), seeker, null)
                .andExpect(jsonPath("$.content[?(@.id == " + postId + ")].commentCount", contains(1)))
                .andExpect(jsonPath("$.content[?(@.id == " + postId + ")].canEdit", contains(true)));
        call(delete("/api/comments/" + commentId), seeker, null).andExpect(status().isForbidden());
        call(put("/api/comments/" + commentId), referrer, Map.of("content", "Edited")).andExpect(status().isOk());

        // Main admin appoints a city head, who manages the WhatsApp group and bank details
        String admin = login("9999999999", "admin@123");
        long referrerId = json.readTree(call(get("/api/users/me"), referrer, null).andReturn()
                .getResponse().getContentAsString()).get("id").asLong();
        call(post("/api/admin/city-admins"), seeker, Map.of("userId", referrerId, "cityId", delhi))
                .andExpect(status().isForbidden());
        call(post("/api/admin/city-admins"), admin, Map.of("userId", referrerId, "cityId", delhi))
                .andExpect(jsonPath("$.role").value("CITY_ADMIN"));
        Map<String, Object> settings = Map.of("whatsappGroupUrl", "https://chat.whatsapp.com/abc123",
                "bankAccountName", "Kamsar o Bar Delhi", "bankAccountNumber", "1234567890",
                "bankIfsc", "sbin0001234", "upiId", "kob.delhi@okaxis");
        call(put("/api/cities/" + delhi + "/settings"), seeker, settings).andExpect(status().isForbidden());
        call(put("/api/cities/" + mumbai + "/settings"), referrer, settings).andExpect(status().isForbidden());
        call(put("/api/cities/" + delhi + "/settings"), referrer, settings)
                .andExpect(jsonPath("$.bank.ifsc").value("SBIN0001234"));
        call(get("/api/cities/" + delhi), seeker, null)
                .andExpect(jsonPath("$.whatsappGroupUrl").value("https://chat.whatsapp.com/abc123"));
        // a city admin may moderate their own city's content
        call(delete("/api/posts/" + postId), referrer, null).andExpect(status().isNoContent());

        // Donations: recorded as pending, counted once the city admin verifies them
        long campaignId = id(call(post("/api/cities/" + delhi + "/campaigns"), referrer,
                Map.of("title", "Winter blankets", "goalAmount", 10000)).andExpect(status().isCreated()));
        long donationId = id(call(post("/api/donations"), seeker,
                Map.of("cityId", delhi, "campaignId", campaignId, "amount", 500, "transactionRef", "UPI123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING")));
        JsonNode before = summaryFor(delhi);
        org.assertj.core.api.Assertions.assertThat(before.get("collected").decimalValue()).isZero();
        org.assertj.core.api.Assertions.assertThat(before.get("pending").decimalValue()).isEqualByComparingTo("500");

        call(patch("/api/donations/" + donationId + "/status"), seeker, Map.of("status", "VERIFIED"))
                .andExpect(status().isForbidden());
        call(patch("/api/donations/" + donationId + "/status"), referrer, Map.of("status", "VERIFIED"))
                .andExpect(jsonPath("$.status").value("VERIFIED"));
        org.assertj.core.api.Assertions.assertThat(summaryFor(delhi).get("collected").decimalValue())
                .isEqualByComparingTo("500");
        call(get("/api/cities/" + delhi + "/campaigns"), seeker, null)
                .andExpect(jsonPath("$[0].raisedAmount").value(500.0));
        call(get("/api/cities/" + delhi + "/supporters"), seeker, null)
                .andExpect(jsonPath("$.content[0].name").value("Asif Khan"));

        // Revoking removes city-admin powers immediately
        call(delete("/api/admin/city-admins/" + referrerId), admin, null)
                .andExpect(jsonPath("$.role").value("MEMBER"));
        call(get("/api/cities/" + delhi + "/donations"), referrer, null).andExpect(status().isForbidden());
    }

    private JsonNode summaryFor(long cityId) throws Exception {
        JsonNode summary = json.readTree(mvc.perform(get("/api/donations/summary")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        for (JsonNode city : summary.get("cities")) {
            if (city.get("cityId").asLong() == cityId) {
                return city;
            }
        }
        throw new AssertionError("City missing from summary");
    }

    private long cityId(String name) throws Exception {
        JsonNode cities = json.readTree(mvc.perform(get("/api/cities")).andReturn().getResponse()
                .getContentAsString());
        for (JsonNode city : cities) {
            if (city.get("name").asText().equals(name)) {
                return city.get("id").asLong();
            }
        }
        throw new AssertionError("City not seeded: " + name);
    }

    private String register(String name, String mobile, long cityId) throws Exception {
        return token(call(post("/api/auth/register"), null,
                Map.of("name", name, "mobile", mobile, "cityId", cityId, "password", "secret123"))
                .andExpect(status().isCreated()));
    }

    private String login(String mobile, String password) throws Exception {
        return token(call(post("/api/auth/login"), null, Map.of("mobile", mobile, "password", password))
                .andExpect(status().isOk()));
    }

    private String token(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString()).get("token").asText();
    }

    private long id(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String token, Object body) throws Exception {
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return mvc.perform(request);
    }
}
