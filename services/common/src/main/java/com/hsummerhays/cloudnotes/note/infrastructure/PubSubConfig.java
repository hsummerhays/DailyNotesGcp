package com.hsummerhays.cloudnotes.note.infrastructure;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PubSubConfig {

    private static final Logger log = LoggerFactory.getLogger(PubSubConfig.class);

    private final PubSubAdmin pubSubAdmin;

    public PubSubConfig(PubSubAdmin pubSubAdmin) {
        this.pubSubAdmin = pubSubAdmin;
    }

    /**
     * Runs during bean initialization (context refresh), which Spring guarantees completes
     * before the ApplicationReadyEvent fires for any listener - including subscribers like
     * ImportMessageConsumer that must not start pulling until this subscription exists.
     */
    @PostConstruct
    public void ensureTopicAndSubscriptionExist() {
        try {
            if (pubSubAdmin.getTopic("notes-import-topic") == null) {
                pubSubAdmin.createTopic("notes-import-topic");
            }
            if (pubSubAdmin.getSubscription("notes-import-sub") == null) {
                pubSubAdmin.createSubscription("notes-import-sub", "notes-import-topic");
            }
        } catch (Exception e) {
            log.warn("Could not verify/create Pub/Sub topic/subscription", e);
        }
    }
}
