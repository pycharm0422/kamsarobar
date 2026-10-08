package com.kamsarobar;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/** City admins cannot move themselves; one admin per city; moving a city admin moves their admin role. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CityAdminRulesIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void cityAdminRules() throws Exception {
        long varanasi = cityId("Varanasi");
        long gurugram = cityId("Gurugram");
        String admin = login("9999999999", "admin@123");
        String head = register("Varanasi Head", "9600000001", varanasi);
        String other = register("Gurugram Head", "9600000002", gurugram);
        String member = register("Plain Member", "9600000003", varanasi);
        long headId = meId(head);
        long otherId = meId(other);
        long memberId = meId(member);
        call(post("/api/admin/city-admins"), admin, Map.of("userId", headId, "cityId", varanasi))
                .andExpect(jsonPath("$.managedCity.name").value("Varanasi"));
        call(post("/api/admin/city-admins"), admin, Map.of("userId", otherId, "cityId", gurugram))
                .andExpect(status().isOk());

        // A city admin cannot change their own city (but can still edit name / mobile); a member can.
        call(put("/api/users/me"), head, Map.of("name", "Varanasi Head", "mobile", "9600000001", "cityId", gurugram))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("only the main admin")));
        call(put("/api/users/me"), head, Map.of("name", "Varanasi Head Renamed", "mobile", "9600000001", "cityId", varanasi))
                .andExpect(jsonPath("$.name").value("Varanasi Head Renamed"));
        call(put("/api/users/me"), member, Map.of("name", "Plain Member", "mobile", "9600000003", "cityId", gurugram))
                .andExpect(jsonPath("$.city.name").value("Gurugram"));

        // Only the main admin may move people.
        call(put("/api/admin/members/" + memberId + "/city"), head, Map.of("cityId", varanasi))
                .andExpect(status().isForbidden());
        call(get("/api/admin/members/" + headId), admin, null).andExpect(jsonPath("$.canChangeCity").value(true));
        call(get("/api/admin/members/" + memberId), other, null).andExpect(jsonPath("$.canChangeCity").value(false));

        // One admin per city: appointing someone to a city that has an admin needs explicit replacement.
        call(post("/api/admin/city-admins"), admin, Map.of("userId", memberId, "cityId", varanasi))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CITY_HAS_ADMIN"))
                .andExpect(jsonPath("$.message").value("Varanasi already has an admin: Varanasi Head Renamed."));

        // Moving the Gurugram admin to Varanasi: blocked first, then replaces the Varanasi admin when confirmed.
        call(put("/api/admin/members/" + otherId + "/city"), admin, Map.of("cityId", varanasi))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CITY_HAS_ADMIN"));
        call(get("/api/users/me"), other, null).andExpect(jsonPath("$.city.name").value("Gurugram")); // unchanged
        call(put("/api/admin/members/" + otherId + "/city"), admin, Map.of("cityId", varanasi, "replaceExistingAdmin", true))
                .andExpect(jsonPath("$.city.name").value("Varanasi"))
                .andExpect(jsonPath("$.managedCity.name").value("Varanasi"))
                .andExpect(jsonPath("$.role").value("CITY_ADMIN"));
        call(get("/api/users/me"), head, null)
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.managedCity").doesNotExist());
        call(get("/api/admin/members?cityId=" + varanasi), head, null).andExpect(status().isForbidden()); // no longer admin

        // Gurugram now has no admin, so someone can be appointed there without replacing anyone.
        call(post("/api/admin/city-admins"), admin, Map.of("userId", memberId, "cityId", gurugram))
                .andExpect(jsonPath("$.managedCity.name").value("Gurugram"));

        // Moving a regular member just changes where they live.
        call(put("/api/admin/members/" + headId + "/city"), admin, Map.of("cityId", gurugram))
                .andExpect(jsonPath("$.city.name").value("Gurugram"))
                .andExpect(jsonPath("$.role").value("MEMBER"));
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
