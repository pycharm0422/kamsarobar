package com.kamsarobar;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** A member who moves to another city takes their "only my city" posts along; other posts stay where they are. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberMoveIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void cityOnlyPostsMoveWithTheMember() throws Exception {
        long bengaluru = cityId("Bengaluru");
        long dubai = cityId("Dubai");
        long dildarnagar = cityId("Dildarnagar");
        String admin = login("9999999999", "admin@123");
        String mover = register("Moving Member", "9300000001", bengaluru);
        String oldNeighbour = register("Bengaluru Neighbour", "9300000002", bengaluru);
        String newNeighbour = register("Dubai Neighbour", "9300000003", dubai);
        String laterNeighbour = register("Dildarnagar Neighbour", "9300000004", dildarnagar);
        String start = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();

        long cityOnly = postId(mover, Map.of("content", "Bengaluru-only chat", "visibility", "CITY"));
        long forEveryone = postId(mover, Map.of("content", "Hello everyone", "visibility", "EVERYONE"));
        long upcomingEvent = postId(mover, Map.of("category", "EVENT", "visibility", "CITY", "title", "Bengaluru meetup",
                "eventStartsAt", start));
        long finishedEvent = postId(mover, Map.of("category", "EVENT", "visibility", "CITY", "title", "Old meetup",
                "eventStartsAt", start));
        jdbc.update("update posts set event_starts_at = ? where id = ?",
                java.sql.Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS)), finishedEvent);
        call(put("/api/posts/" + upcomingEvent + "/attendance"), oldNeighbour, null).andExpect(status().isOk());

        // The member moves themselves.
        call(put("/api/users/me"), mover, Map.of("name", "Moving Member", "mobile", "9300000001", "cityId", dubai))
                .andExpect(jsonPath("$.city.name").value("Dubai"));

        // The city-only post and the finished event now belong to Dubai: Dubai sees them, Bengaluru no longer does.
        for (long moved : new long[] {cityOnly, finishedEvent}) {
            call(get("/api/posts/" + moved), newNeighbour, null).andExpect(jsonPath("$.city.name").value("Dubai"));
            call(get("/api/posts/" + moved), oldNeighbour, null).andExpect(status().isNotFound());
        }
        // Shared-with-everyone posts and events that haven't happened yet stay in Bengaluru.
        call(get("/api/posts/" + forEveryone), oldNeighbour, null).andExpect(jsonPath("$.city.name").value("Bengaluru"));
        call(get("/api/posts/" + upcomingEvent), oldNeighbour, null)
                .andExpect(jsonPath("$.city.name").value("Bengaluru"))
                .andExpect(jsonPath("$.event.attending").value(true));
        call(get("/api/posts/" + upcomingEvent), newNeighbour, null).andExpect(status().isNotFound());
        // Moving doesn't mark posts as edited, and the author still sees and manages all of them.
        call(get("/api/posts/" + cityOnly), mover, null)
                .andExpect(jsonPath("$.content").value("Bengaluru-only chat"));
        call(get("/api/posts/" + upcomingEvent), mover, null).andExpect(status().isOk());

        // The same happens when the main admin moves someone.
        long moverId = meId(mover);
        call(put("/api/admin/members/" + moverId + "/city"), admin, Map.of("cityId", dildarnagar))
                .andExpect(jsonPath("$.city.name").value("Dildarnagar"));
        call(get("/api/posts/" + cityOnly), laterNeighbour, null).andExpect(jsonPath("$.city.name").value("Dildarnagar"));
        call(get("/api/posts/" + cityOnly), newNeighbour, null).andExpect(status().isNotFound());
    }

    private long postId(String token, Map<String, Object> post) throws Exception {
        return body(call(post("/api/posts"), token, post).andExpect(status().isCreated())).get("id").asLong();
    }

    private long meId(String token) throws Exception {
        return body(call(get("/api/users/me"), token, null)).get("id").asLong();
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
