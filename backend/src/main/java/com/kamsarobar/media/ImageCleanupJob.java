package com.kamsarobar.media;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Hourly sweep of photos that are not attached to any post for more than a day. */
@Component
public class ImageCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ImageCleanupJob.class);

    private final ImageService imageService;

    public ImageCleanupJob(ImageService imageService) {
        this.imageService = imageService;
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    public void run() {
        int deleted = imageService.deleteOrphans(LocalDateTime.now().minusDays(1));
        if (deleted > 0) {
            log.info("Deleted {} unused photos", deleted);
        }
    }
}
