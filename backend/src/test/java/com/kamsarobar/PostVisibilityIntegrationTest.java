package com.kamsarobar;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

/** "Only my city" posts must be invisible to other cities everywhere: feed, direct link, comments and events. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostVisibilityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void cityOnlyPostsStayInTheirCity() throws Exception {
        long patna = cityId("Patna");
        long kolkata = cityId("Kolkata");
        String author = register("Faiz Khan", "9200000001", patna);
        String neighbour = register("Sana Khan", "9200000002", patna);
        String outsider = register("Zaid Khan", "9200000003", kolkata);

        long cityOnly = id(call(post("/api/posts"), author, Map.of("content", "Patna-only update"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visibility").value("CITY"))); // default
        long forAll = id(call(post("/api/posts"), author,
                Map.of("content", "Hello everyone", "visibility", "EVERYONE"))
                .andExpect(jsonPath("$.visibility").value("EVERYONE")));

        // Same city: sees both. Other city: only the post shared with everyone - in every listing.
        call(get("/api/posts"), neighbour, null).andExpect(jsonPath("$.content[*].id", hasItem((int) cityOnly)));
        call(get("/api/posts"), outsider, null)
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) cityOnly))))
                .andExpect(jsonPath("$.content[*].id", hasItem((int) forAll)));
        call(get("/api/posts?cityId=" + patna), outsider, null)
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) cityOnly))));
        call(get("/api/posts?cityId=" + patna + "&category=GENERAL"), outsider, null)
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) cityOnly))))
                .andExpect(jsonPath("$.content[*].id", hasItem((int) forAll)));

        // Direct link and comments behave as if the post does not exist.
        call(get("/api/posts/" + cityOnly), outsider, null).andExpect(status().isNotFound());
        call(get("/api/posts/" + cityOnly + "/comments"), outsider, null).andExpect(status().isNotFound());
        call(post("/api/posts/" + cityOnly + "/comments"), outsider, Map.of("content", "hi"))
                .andExpect(status().isNotFound());
        call(post("/api/posts/" + cityOnly + "/comments"), neighbour, Map.of("content", "Nice"))
                .andExpect(status().isCreated());

        // Events: a city-only seminar is hidden from other cities, and they cannot add it to their list.
        String start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        long seminar = id(call(post("/api/posts"), author, Map.of("category", "SEMINAR", "title", "Patna meetup",
                "eventStartsAt", start, "visibility", "CITY")).andExpect(status().isCreated()));
        call(get("/api/events/upcoming"), outsider, null)
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) seminar))));
        call(put("/api/posts/" + seminar + "/attendance"), outsider, null).andExpect(status().isNotFound());
        call(put("/api/posts/" + seminar + "/attendance"), neighbour, null)
                .andExpect(jsonPath("$.event.attending").value(true));

        // Switching it to everyone makes it visible to all.
        call(put("/api/posts/" + cityOnly), author, Map.of("content", "Now for all", "visibility", "EVERYONE"))
                .andExpect(jsonPath("$.visibility").value("EVERYONE"));
        call(get("/api/posts/" + cityOnly), outsider, null).andExpect(status().isOk());

        // The main admin sees everything; a city admin sees the city they manage even if they live elsewhere.
        String admin = login("9999999999", "admin@123");
        call(get("/api/posts/" + seminar), admin, null).andExpect(status().isOk());
        long outsiderId = body(call(get("/api/users/me"), outsider, null)).get("id").asLong();
        call(post("/api/admin/city-admins"), admin, Map.of("userId", outsiderId, "cityId", patna))
                .andExpect(status().isOk());
        call(get("/api/posts/" + seminar), outsider, null).andExpect(status().isOk());

        // An author who moves away still sees their own city-only post.
        call(put("/api/users/me"), author, Map.of("name", "Faiz Khan", "mobile", "9200000001", "cityId", kolkata))
                .andExpect(status().isOk());
        call(get("/api/posts/" + seminar), author, null).andExpect(status().isOk());
    }

    private long cityId(String name) throws Exception {
        for (JsonNode city : body(mvc.perform(get("/api/cities")))) {
            if (city.get("name").asText().equals(name)) {
                return city.get("id").asLong();
            }
        }
        throw new AssertionError("City not seeded: " + name);
    }

    private String register(String name, String mobile, long cityId) throws Exception {
        return body(call(post("/api/auth/register"), null,
                Map.of("name", name, "mobile", mobile, "cityId", cityId, "password", "secret123"))
                .andExpect(status().isCreated())).get("token").asText();
    }

    private String login(String mobile, String password) throws Exception {
        return body(call(post("/api/auth/login"), null, Map.of("mobile", mobile, "password", password))
                .andExpect(status().isOk())).get("token").asText();
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private long id(ResultActions result) throws Exception {
        return body(result).get("id").asLong();
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String token, Object payload) throws Exception {
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (payload != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload));
        }
        return mvc.perform(request);
    }
}
