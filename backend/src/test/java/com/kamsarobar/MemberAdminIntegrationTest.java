package com.kamsarobar;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/** Admins view member profiles and block / unblock members; blocked members disappear everywhere. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberAdminIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void viewAndBlockMembers() throws Exception {
        long lucknow = cityId("Lucknow");
        long noida = cityId("Noida");
        String admin = login("9999999999", "admin@123");
        String head = register("Head Lucknow", "9400000001", lucknow);
        String spammer = register("Spam Account", "9400000002", lucknow);
        String member = register("Rehan Khan", "9400000003", lucknow);
        String outsider = register("Noida Member", "9400000004", noida);
        long headId = meId(head);
        long spammerId = meId(spammer);
        long memberId = meId(member);
        long outsiderId = meId(outsider);
        call(post("/api/admin/city-admins"), admin, Map.of("userId", headId, "cityId", lucknow))
                .andExpect(status().isOk());

        // The spammer has a profile, a public post and a comment on someone else's post.
        call(put("/api/profile/me"), spammer, Map.of("currentCompany", "Infosys",
                "referralCompanies", List.of("Infosys"), "expertise", List.of("Crypto"))).andExpect(status().isOk());
        long spamPost = id(call(post("/api/posts"), spammer,
                Map.of("content", "Earn 1 lakh per day!!!", "visibility", "EVERYONE")));
        long memberPost = id(call(post("/api/posts"), member, Map.of("content", "Hello Lucknow")));
        call(post("/api/posts/" + memberPost + "/comments"), spammer, Map.of("content", "Click my link"))
                .andExpect(status().isCreated());

        // City admin sees only their city's members, and the full profile with activity.
        call(get("/api/admin/members"), head, null)
                .andExpect(jsonPath("$.content[*].id", hasItem((int) spammerId)))
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) outsiderId))));
        call(get("/api/admin/members?cityId=" + noida), head, null).andExpect(status().isForbidden());
        call(get("/api/admin/members/" + outsiderId), head, null).andExpect(status().isForbidden());
        call(get("/api/admin/members/" + spammerId), head, null)
                .andExpect(jsonPath("$.member.name").value("Spam Account"))
                .andExpect(jsonPath("$.profile.referralCompanies[0]").value("Infosys"))
                .andExpect(jsonPath("$.activity.posts").value(1))
                .andExpect(jsonPath("$.activity.comments").value(1))
                .andExpect(jsonPath("$.canBlock").value(true));
        call(get("/api/admin/members"), member, null).andExpect(status().isForbidden()); // members can't

        // Who may block whom.
        call(post("/api/admin/members/" + outsiderId + "/block"), head, Map.of("reason", "x"))
                .andExpect(status().isForbidden());
        call(post("/api/admin/members/" + headId + "/block"), head, Map.of("reason", "x"))
                .andExpect(status().isBadRequest()); // yourself
        call(post("/api/admin/members/" + spammerId + "/block"), head, Map.of("reason", ""))
                .andExpect(status().isBadRequest()); // reason required

        // Block: logged out at once, cannot log in or re-register, hidden from search, posts and comments.
        call(post("/api/admin/members/" + spammerId + "/block"), head, Map.of("reason", "Spam links"))
                .andExpect(jsonPath("$.block.blocked").value(true))
                .andExpect(jsonPath("$.block.reason").value("Spam links"))
                .andExpect(jsonPath("$.block.blockedBy").value("Head Lucknow"));
        call(get("/api/users/me"), spammer, null).andExpect(status().isUnauthorized());
        call(post("/api/auth/login"), null, Map.of("mobile", "9400000002", "password", "secret123"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("blocked")));
        call(post("/api/auth/register"), null, Map.of("name", "Again", "mobile", "9400000002",
                "cityId", noida, "password", "secret123")).andExpect(status().isConflict());
        call(get("/api/directory/referrers?company=infosys"), outsider, null)
                .andExpect(jsonPath("$.content[*].userId", not(hasItem((int) spammerId))));
        call(get("/api/posts"), outsider, null).andExpect(jsonPath("$.content[*].id", not(hasItem((int) spamPost))));
        call(get("/api/posts/" + spamPost), admin, null).andExpect(status().isNotFound());
        call(get("/api/posts/" + memberPost), member, null).andExpect(jsonPath("$.commentCount").value(0));
        call(get("/api/posts/" + memberPost + "/comments"), member, null)
                .andExpect(jsonPath("$.totalElements").value(0));
        call(get("/api/admin/members?status=BLOCKED"), head, null)
                .andExpect(jsonPath("$.content[*].id", hasItem((int) spammerId)))
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) memberId))));

        // A city admin cannot block another admin (here the admin of Noida who lives in Lucknow).
        call(post("/api/admin/city-admins"), admin, Map.of("userId", memberId, "cityId", noida))
                .andExpect(status().isOk());
        call(post("/api/admin/members/" + memberId + "/block"), head, Map.of("reason", "x"))
                .andExpect(status().isForbidden());
        call(get("/api/admin/members/" + memberId), head, null).andExpect(jsonPath("$.canBlock").value(false));

        // Unblock fully restores the account and its content.
        call(delete("/api/admin/members/" + spammerId + "/block"), admin, null)
                .andExpect(jsonPath("$.block.blocked").value(false));
        call(post("/api/auth/login"), null, Map.of("mobile", "9400000002", "password", "secret123"))
                .andExpect(status().isOk());
        call(get("/api/posts/" + spamPost), outsider, null).andExpect(status().isOk());
        call(get("/api/posts/" + memberPost), member, null).andExpect(jsonPath("$.commentCount").value(1));
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
