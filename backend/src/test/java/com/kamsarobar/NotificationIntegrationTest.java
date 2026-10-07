package com.kamsarobar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kamsarobar.notification.NotificationService;
import com.kamsarobar.notification.PushMessage;
import com.kamsarobar.notification.PushSender;

/** Who gets which push notification, settings, invalid-token cleanup and event reminders. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(NotificationIntegrationTest.Recording.class)
class NotificationIntegrationTest {

    static final List<PushMessage> SENT = new CopyOnWriteArrayList<>();
    static final Set<String> DEAD_TOKENS = Set.of("ExponentPushToken[uninstalled]");

    @TestConfiguration
    static class Recording {
        @Bean
        @Primary
        PushSender recordingPushSender() {
            return messages -> {
                SENT.addAll(messages);
                return messages.stream().map(PushMessage::to).filter(DEAD_TOKENS::contains)
                        .collect(java.util.stream.Collectors.toSet());
            };
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private NotificationService notificationService;

    @BeforeEach
    void reset() {
        SENT.clear();
    }

    @Test
    void notifiesTheRightPeople() throws Exception {
        long chennai = cityId("Chennai");
        long hyderabad = cityId("Hyderabad");
        String author = register("Arif Khan", "9500000001", chennai);
        String neighbour = register("Nadia Khan", "9500000002", chennai);
        String quiet = register("Quiet Khan", "9500000003", chennai);
        String faraway = register("Imtiaz Khan", "9500000004", hyderabad);
        device(author, "ExponentPushToken[author]");
        device(neighbour, "ExponentPushToken[neighbour]");
        device(neighbour, "ExponentPushToken[uninstalled]"); // a second, dead phone
        device(quiet, "ExponentPushToken[quiet]");
        device(faraway, "ExponentPushToken[faraway]");
        call(put("/api/notifications/devices"), neighbour, Map.of("token", "not-a-token"))
                .andExpect(status().isBadRequest());

        // "quiet" turns post notifications off; "faraway" opts in to events from all cities.
        call(put("/api/notifications/settings"), quiet, Map.of("cityPosts", false, "cityEvents", true,
                "allEvents", false, "eventReminders", true)).andExpect(jsonPath("$.cityPosts").value(false));
        call(put("/api/notifications/settings"), faraway, Map.of("cityPosts", true, "cityEvents", true,
                "allEvents", true, "eventReminders", true)).andExpect(status().isOk());
        call(get("/api/notifications/settings"), neighbour, null)
                .andExpect(jsonPath("$.cityPosts").value(true))
                .andExpect(jsonPath("$.allEvents").value(false));

        // New post: only Chennai members who want post notifications, never the author.
        call(post("/api/posts"), author, Map.of("title", "Iftar get-together", "content", "This Saturday",
                "visibility", "EVERYONE")).andExpect(status().isCreated());
        List<PushMessage> postPush = await(m -> m.title().contains("Arif Khan"));
        assertThat(postPush).extracting(PushMessage::to)
                .containsExactlyInAnyOrder("ExponentPushToken[neighbour]", "ExponentPushToken[uninstalled]");
        assertThat(postPush.get(0).body()).isEqualTo("Iftar get-together - This Saturday");
        assertThat(postPush.get(0).data()).containsEntry("type", "post");

        // The dead token was removed, so the next notification skips it.
        SENT.clear();
        String start = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        long seminar = id(call(post("/api/posts"), author, Map.of("category", "SEMINAR", "title", "Tech career talk",
                "eventStartsAt", start, "eventLocation", "Anna Nagar", "visibility", "EVERYONE")));
        List<PushMessage> eventPush = await(m -> m.title().startsWith("📅"));
        assertThat(eventPush).extracting(PushMessage::to).containsExactlyInAnyOrder(
                "ExponentPushToken[neighbour]", "ExponentPushToken[quiet]", "ExponentPushToken[faraway]");
        assertThat(eventPush.get(0).title()).isEqualTo("📅 New seminar in Chennai");
        assertThat(eventPush.get(0).body()).isEqualTo("Tech career talk · Anna Nagar");
        assertThat(eventPush.get(0).data()).containsEntry("postId", seminar);

        // A city-only event does not reach other cities, even if they opted in.
        SENT.clear();
        call(post("/api/posts"), author, Map.of("category", "EVENT", "title", "Chennai-only meetup",
                "eventStartsAt", start, "visibility", "CITY")).andExpect(status().isCreated());
        assertThat(await(m -> m.body().startsWith("Chennai-only"))).extracting(PushMessage::to)
                .doesNotContain("ExponentPushToken[faraway]");

        // Reminder: an event starting in 30 minutes, added by "neighbour" - reminded exactly once.
        String soon = Instant.now().plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS).toString();
        long meetup = id(call(post("/api/posts"), author, Map.of("category", "EVENT", "title", "Cricket match",
                "eventStartsAt", soon, "visibility", "CITY")));
        call(put("/api/posts/" + meetup + "/attendance"), neighbour, null).andExpect(status().isOk());
        await(m -> m.body().startsWith("Cricket match"));
        SENT.clear();
        notificationService.sendEventReminders();
        notificationService.sendEventReminders();
        List<PushMessage> reminders = SENT.stream().filter(m -> m.title().equals("⏰ Starting soon")).toList();
        assertThat(reminders).hasSize(1);
        assertThat(reminders.get(0).to()).isEqualTo("ExponentPushToken[neighbour]");
        assertThat(reminders.get(0).body()).matches("Cricket match starts in (29|30) min");

        // Logging out removes the phone.
        call(delete("/api/notifications/devices?token=ExponentPushToken[neighbour]"), neighbour, null)
                .andExpect(status().isNoContent());
        SENT.clear();
        call(post("/api/posts"), author, Map.of("content", "One more update")).andExpect(status().isCreated());
        Thread.sleep(500);
        assertThat(SENT).extracting(PushMessage::to).doesNotContain("ExponentPushToken[neighbour]");
    }

    /** Notifications are sent in the background; wait up to 5 seconds for matching ones. */
    private List<PushMessage> await(Predicate<PushMessage> match) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            List<PushMessage> found = SENT.stream().filter(match).toList();
            if (!found.isEmpty()) {
                Thread.sleep(100); // let the rest of the batch arrive
                return SENT.stream().filter(match).toList();
            }
            Thread.sleep(100);
        }
        throw new AssertionError("No matching notification was sent. Sent: " + SENT);
    }

    private void device(String token, String pushToken) throws Exception {
        call(put("/api/notifications/devices"), token, Map.of("token", pushToken, "platform", "android"))
                .andExpect(status().isNoContent());
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
