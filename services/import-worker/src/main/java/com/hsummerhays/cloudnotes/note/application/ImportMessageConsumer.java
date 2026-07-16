package com.hsummerhays.cloudnotes.note.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.hsummerhays.cloudnotes.note.domain.ImportRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Service
public class ImportMessageConsumer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(ImportMessageConsumer.class);

    private final PubSubTemplate pubSubTemplate;
    private final ImportJobProcessor importJobProcessor;
    private final ObjectMapper objectMapper;

    public ImportMessageConsumer(
            PubSubTemplate pubSubTemplate,
            ImportJobProcessor importJobProcessor,
            ObjectMapper objectMapper
    ) {
        this.pubSubTemplate = pubSubTemplate;
        this.importJobProcessor = importJobProcessor;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Starting Pub/Sub listener on subscription: notes-import-sub");
        pubSubTemplate.subscribe("notes-import-sub", this::handleMessage);
    }

    private void handleMessage(BasicAcknowledgeablePubsubMessage message) {
        String payload = message.getPubsubMessage().getData().toStringUtf8();
        log.info("Received Pub/Sub message: {}", payload);

        try {
            ImportRequestedEvent event = objectMapper.readValue(payload, ImportRequestedEvent.class);

            importJobProcessor.processJob(event.jobId());

            // Acknowledge the message upon successful execution
            message.ack();
            log.info("Successfully acknowledged message for jobId: {}", event.jobId());
        } catch (Exception e) {
            log.error("Failed to process Pub/Sub import message, nacking message: {}", payload, e);
            message.nack(); // Nack triggers retry / dead-lettering
        }
    }
}
