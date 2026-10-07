package com.kamsarobar.notification;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** For tests / offline development (app.push.provider=log): writes notifications to the log instead. */
@Component
@ConditionalOnProperty(name = "app.push.provider", havingValue = "log")
public class LoggingPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushSender.class);

    @Override
    public Set<String> send(List<PushMessage> messages) {
        messages.forEach(m -> log.info("[push] to={} title='{}' body='{}' data={}", m.to(), m.title(), m.body(),
                m.data()));
        return Set.of();
    }
}
