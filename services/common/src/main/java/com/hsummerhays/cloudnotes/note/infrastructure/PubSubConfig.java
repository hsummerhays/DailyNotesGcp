package com.hsummerhays.cloudnotes.note.infrastructure;

import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.protobuf.Duration;
import com.google.pubsub.v1.DeadLetterPolicy;
import com.google.pubsub.v1.RetryPolicy;
import com.google.pubsub.v1.Subscription;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PubSubConfig {

    private static final Logger log = LoggerFactory.getLogger(PubSubConfig.class);

    private final PubSubAdmin pubSubAdmin;
    private final GcpProjectIdProvider projectIdProvider;

    public PubSubConfig(PubSubAdmin pubSubAdmin, GcpProjectIdProvider projectIdProvider) {
        this.pubSubAdmin = pubSubAdmin;
        this.projectIdProvider = projectIdProvider;
    }

    /**
     * Runs during bean initialization (context refresh), which Spring guarantees completes
     * before the ApplicationReadyEvent fires for any listener - including subscribers like
     * ImportMessageConsumer that must not start pulling until this subscription exists.
     */
    @PostConstruct
    public void ensureTopicAndSubscriptionExist() {
        try {
            String projectId = projectIdProvider.getProjectId();
            log.info("Ensuring topics and subscriptions exist for project: {}", projectId);

            // 1. Ensure primary topic exists
            if (pubSubAdmin.getTopic("notes-import-topic") == null) {
                pubSubAdmin.createTopic("notes-import-topic");
                log.info("Created topic: notes-import-topic");
            }

            // 2. Ensure dead-letter topic and its subscriber subscription exist
            if (pubSubAdmin.getTopic("notes-import-dl-topic") == null) {
                pubSubAdmin.createTopic("notes-import-dl-topic");
                log.info("Created DLQ topic: notes-import-dl-topic");
            }
            if (pubSubAdmin.getSubscription("notes-import-dl-sub") == null) {
                pubSubAdmin.createSubscription("notes-import-dl-sub", "notes-import-dl-topic");
                log.info("Created DLQ subscription: notes-import-dl-sub");
            }

            // 3. Ensure primary subscription exists with retry and dead-letter policies
            if (pubSubAdmin.getSubscription("notes-import-sub") == null) {
                String mainTopicPath = String.format("projects/%s/topics/%s", projectId, "notes-import-topic");
                String dlTopicPath = String.format("projects/%s/topics/%s", projectId, "notes-import-dl-topic");
                String mainSubPath = String.format("projects/%s/subscriptions/%s", projectId, "notes-import-sub");

                Subscription.Builder subscriptionBuilder = Subscription.newBuilder()
                        .setName(mainSubPath)
                        .setTopic(mainTopicPath)
                        .setAckDeadlineSeconds(30)
                        .setDeadLetterPolicy(DeadLetterPolicy.newBuilder()
                                .setDeadLetterTopic(dlTopicPath)
                                .setMaxDeliveryAttempts(5)
                                .build())
                        .setRetryPolicy(RetryPolicy.newBuilder()
                                .setMinimumBackoff(Duration.newBuilder().setSeconds(10).build())
                                .setMaximumBackoff(Duration.newBuilder().setSeconds(600).build())
                                .build());

                pubSubAdmin.createSubscription(subscriptionBuilder);
                log.info("Created subscription 'notes-import-sub' with Dead-Letter Policy (to {}) and Exponential Backoff", dlTopicPath);
            }
        } catch (Exception e) {
            log.error("Could not verify/create Pub/Sub topics/subscriptions", e);
        }
    }
}

