package com.kamsarobar.notification;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.kamsarobar.config.AppProperties;

/** Sends through https://exp.host - Expo's push service accepts up to 100 messages per request. */
@Component
@ConditionalOnProperty(name = "app.push.provider", havingValue = "expo", matchIfMissing = true)
public class ExpoPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushSender.class);
    private static final int BATCH = 100;

    private final RestClient client;

    public ExpoPushSender(AppProperties properties) {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://exp.host/--/api/v2/push/send");
        String accessToken = properties.push() == null ? null : properties.push().expoAccessToken();
        if (accessToken != null && !accessToken.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + accessToken);
        }
        this.client = builder.build();
    }

    @Override
    public Set<String> send(List<PushMessage> messages) {
        Set<String> invalid = new HashSet<>();
        for (int i = 0; i < messages.size(); i += BATCH) {
            List<PushMessage> batch = messages.subList(i, Math.min(i + BATCH, messages.size()));
            try {
                JsonNode response = client.post().contentType(MediaType.APPLICATION_JSON)
                        .body(toPayload(batch)).retrieve().body(JsonNode.class);
                JsonNode tickets = response == null ? null : response.get("data");
                for (int t = 0; tickets != null && t < tickets.size() && t < batch.size(); t++) {
                    JsonNode ticket = tickets.get(t);
                    if ("error".equals(ticket.path("status").asText())) {
                        String error = ticket.path("details").path("error").asText();
                        if ("DeviceNotRegistered".equals(error)) {
                            invalid.add(batch.get(t).to());
                        } else {
                            log.warn("Push to a device failed: {} {}", error, ticket.path("message").asText());
                        }
                    }
                }
            } catch (RuntimeException ex) {
                log.warn("Could not reach the Expo push service: {}", ex.getMessage());
            }
        }
        return invalid;
    }

    private static List<Map<String, Object>> toPayload(List<PushMessage> batch) {
        List<Map<String, Object>> payload = new ArrayList<>(batch.size());
        for (PushMessage m : batch) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("to", m.to());
            item.put("title", m.title());
            item.put("body", m.body());
            item.put("data", m.data());
            item.put("sound", "default");
            item.put("priority", "high");
            item.put("channelId", "default");
            payload.add(item);
        }
        return payload;
    }
}
