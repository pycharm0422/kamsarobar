package com.kamsarobar.notification;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import com.kamsarobar.post.EventAttendeeRepository;
import com.kamsarobar.post.Post;
import com.kamsarobar.post.PostCategory;
import com.kamsarobar.post.PostCreatedEvent;
import com.kamsarobar.post.PostRepository;
import com.kamsarobar.post.PostVisibility;

/**
 * Decides who is notified and sends the push notifications:
 * <ul>
 *   <li>new post - members living in the post's city (if they want post notifications);</li>
 *   <li>new event / seminar - members of the city, plus members elsewhere who opted in to events from all
 *       cities when the event is shared with everyone;</li>
 *   <li>reminder - about an hour before an event a member added to My upcoming events.</li>
 * </ul>
 * The author and blocked members are never notified. Sending happens in the background after the post is saved.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int PAGE = 500;
    private static final Duration REMINDER_WINDOW = Duration.ofMinutes(60);

    private final PostRepository postRepository;
    private final DeviceTokenRepository tokenRepository;
    private final EventAttendeeRepository attendeeRepository;
    private final PushSender pushSender;
    private final TransactionTemplate readOnly;

    public NotificationService(PostRepository postRepository, DeviceTokenRepository tokenRepository,
                               EventAttendeeRepository attendeeRepository, PushSender pushSender,
                               PlatformTransactionManager transactionManager) {
        this.postRepository = postRepository;
        this.tokenRepository = tokenRepository;
        this.attendeeRepository = attendeeRepository;
        this.pushSender = pushSender;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostCreated(PostCreatedEvent event) {
        try {
            PostSnapshot post = readOnly.execute(tx -> postRepository.findWithAuthorById(event.postId())
                    .map(PostSnapshot::of).orElse(null));
            if (post == null || post.authorBlocked()) {
                return;
            }
            String title = post.isEvent()
                    ? "📅 New " + (post.category() == PostCategory.SEMINAR ? "seminar" : "event") + " in " + post.cityName()
                    : post.authorName() + " · " + post.cityName();
            Map<String, Object> data = Map.of("type", post.isEvent() ? "event" : "post", "postId", post.id());
            long afterId = 0;
            while (true) {
                List<Object[]> page = post.isEvent()
                        ? tokenRepository.findEventAudience(post.cityId(), post.visibility() == PostVisibility.EVERYONE,
                                post.authorId(), afterId, PageRequest.of(0, PAGE))
                        : tokenRepository.findPostAudience(post.cityId(), post.authorId(), afterId, PageRequest.of(0, PAGE));
                if (page.isEmpty()) {
                    break;
                }
                List<PushMessage> messages = new ArrayList<>(page.size());
                for (Object[] row : page) {
                    messages.add(new PushMessage((String) row[1], title, post.summary(), data));
                    afterId = (Long) row[0];
                }
                dropInvalid(pushSender.send(messages));
                if (page.size() < PAGE) {
                    break;
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Could not send notifications for post {}", event.postId(), ex);
        }
    }

    /** Every 5 minutes: remind members about events they added that start within the next hour. */
    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void sendEventReminders() {
        Instant now = Instant.now();
        List<Object[]> due = attendeeRepository.findDueReminders(now, now.plus(REMINDER_WINDOW), PageRequest.of(0, PAGE));
        List<PushMessage> messages = new ArrayList<>();
        for (Object[] row : due) {
            Long postId = (Long) row[0];
            Long userId = (Long) row[1];
            if (attendeeRepository.claimReminder(postId, userId, LocalDateTime.now()) != 1) {
                continue; // another server instance already sent it
            }
            long minutes = Math.max(1, Duration.between(now, (Instant) row[3]).toMinutes());
            String body = (row[2] == null ? "Your event" : (String) row[2]) + " starts in " + minutes + " min";
            for (DeviceToken token : tokenRepository.findByUserId(userId)) {
                messages.add(new PushMessage(token.getToken(), "⏰ Starting soon", body,
                        Map.of("type", "reminder", "postId", postId)));
            }
        }
        if (!messages.isEmpty()) {
            dropInvalid(pushSender.send(messages));
        }
    }

    private void dropInvalid(Set<String> invalidTokens) {
        if (!invalidTokens.isEmpty()) {
            tokenRepository.deleteByTokens(invalidTokens);
        }
    }

    /** The bits of a post needed to notify, read inside a short transaction. */
    record PostSnapshot(Long id, Long authorId, String authorName, boolean authorBlocked, Long cityId,
                        String cityName, PostCategory category, PostVisibility visibility, boolean isEvent,
                        String summary) {

        static PostSnapshot of(Post post) {
            return new PostSnapshot(post.getId(), post.getAuthor().getId(), post.getAuthor().getName(),
                    post.getAuthor().isBlocked(), post.getCity().getId(), post.getCity().getName(),
                    post.getCategory(), post.getVisibility(), post.isEvent(), summarize(post));
        }

        private static String summarize(Post post) {
            StringBuilder text = new StringBuilder();
            if (post.getTitle() != null) {
                text.append(post.getTitle());
            }
            if (post.isEvent() && post.getEventLocation() != null) {
                text.append(" · ").append(post.getEventLocation());
            } else if (post.getContent() != null) {
                text.append(text.isEmpty() ? "" : " - ").append(post.getContent());
            }
            if (text.isEmpty()) {
                text.append(post.getImages().isEmpty() ? "New post" : "📷 Shared a photo");
            }
            String s = text.toString().replaceAll("\\s+", " ").trim();
            return s.length() > 140 ? s.substring(0, 137) + "..." : s;
        }
    }
}
