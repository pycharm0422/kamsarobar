package com.kamsarobar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kamsarobar.media.ImageService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostsEventsAndPhotosIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private ImageService imageService;

    @Test
    void photosPostsAndEvents() throws Exception {
        long pune = cityId("Pune");
        String alice = register("Ayesha Khan", "9100000001", pune);
        String bob = register("Imran Khan", "9100000002", pune);

        // --- Photos: uploaded first, validated by content, served publicly with long caching ---
        JsonNode photo = body(upload(alice, "photo.png", png(40, 30)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.width").value(40))
                .andExpect(jsonPath("$.height").value(30)));
        String photoId = photo.get("id").asText();
        mvc.perform(get("/api" + photo.get("url").asText()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("immutable")));
        upload(alice, "notes.png", "not really an image".getBytes()).andExpect(status().isBadRequest());
        byte[] tooBig = new byte[3 * 1024 * 1024 + 1];
        tooBig[0] = (byte) 0xFF;
        tooBig[1] = (byte) 0xD8;
        tooBig[2] = (byte) 0xFF;
        upload(alice, "big.jpg", tooBig).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Photo must be 3 MB or smaller"));

        // --- Posts can be "anything": a photo with no text is fine, an empty post is not ---
        call(post("/api/posts"), alice, Map.of()).andExpect(status().isBadRequest());
        long photoPost = id(call(post("/api/posts"), alice, Map.of("imageIds", List.of(photoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.images[0].id").value(photoId))
                .andExpect(jsonPath("$.event").doesNotExist()));
        // someone else cannot reuse a photo that belongs to another post
        call(post("/api/posts"), bob, Map.of("content", "hi", "imageIds", List.of(photoId)))
                .andExpect(status().isBadRequest());
        call(put("/api/posts/" + photoPost), alice, Map.of("content", "Changed my mind", "imageIds", List.of()))
                .andExpect(jsonPath("$.images", hasSize(0)));
        // the removed photo is now an orphan and gets cleaned up
        assertThat(imageService.deleteOrphans(LocalDateTime.now().plusMinutes(1))).isGreaterThanOrEqualTo(1);
        mvc.perform(get("/api/images/" + photoId)).andExpect(status().isNotFound());

        // --- Seminars / events ---
        String start = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        Map<String, Object> seminar = new HashMap<>(Map.of("category", "SEMINAR", "title", "Career seminar",
                "content", "Resume and interview tips", "eventLocation", "Community hall, Pune"));
        call(post("/api/posts"), alice, seminar).andExpect(status().isBadRequest()); // no date
        seminar.put("eventStartsAt", "2020-01-01T10:00:00Z");
        call(post("/api/posts"), alice, seminar).andExpect(status().isBadRequest()); // in the past
        seminar.put("eventStartsAt", start);
        long seminarId = id(call(post("/api/posts"), alice, seminar).andExpect(status().isCreated())
                .andExpect(jsonPath("$.event.startsAt").value(start))
                .andExpect(jsonPath("$.event.attendeeCount").value(0))
                .andExpect(jsonPath("$.event.attending").value(false)));

        call(put("/api/posts/" + seminarId + "/attendance"), bob, null)
                .andExpect(jsonPath("$.event.attending").value(true))
                .andExpect(jsonPath("$.event.attendeeCount").value(1));
        call(put("/api/posts/" + seminarId + "/attendance"), bob, null) // idempotent
                .andExpect(jsonPath("$.event.attendeeCount").value(1));
        call(get("/api/events/mine"), bob, null)
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Career seminar"));
        call(get("/api/events/mine"), alice, null).andExpect(jsonPath("$.content", hasSize(0)));
        call(get("/api/events/upcoming?cityId=" + pune), alice, null)
                .andExpect(jsonPath("$.content[0].id").value(seminarId));
        call(get("/api/posts?cityId=" + pune), bob, null)
                .andExpect(jsonPath("$.content[0].event.attending").value(true));
        call(delete("/api/posts/" + seminarId + "/attendance"), bob, null)
                .andExpect(jsonPath("$.event.attending").value(false));
        call(get("/api/events/mine"), bob, null).andExpect(jsonPath("$.content", hasSize(0)));
        call(put("/api/posts/" + photoPost + "/attendance"), bob, null).andExpect(status().isBadRequest());

        // --- Donations: a single city's amount ---
        call(get("/api/cities/" + pune + "/donation-summary"), alice, null)
                .andExpect(jsonPath("$.cityName").value("Pune"))
                .andExpect(jsonPath("$.collected").value(0));
    }

    private static byte[] png(int w, int h) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    private ResultActions upload(String token, String name, byte[] bytes) throws Exception {
        return mvc.perform(multipart("/api/images").file(new MockMultipartFile("file", name, "image/png", bytes))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
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
