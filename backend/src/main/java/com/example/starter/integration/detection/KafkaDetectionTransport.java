package com.example.starter.integration.detection;

import com.example.starter.model.DetectionResult;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "starter.detection.transport", havingValue = "kafka")
class KafkaDetectionTransport implements DetectionTransport {
    private static final String JOBS_TOPIC = "detection.jobs.v1";
    static final String RESULTS_TOPIC = "detection.results.v1";
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    KafkaDetectionTransport(KafkaTemplate<String, String> kafka, ObjectMapper json) {
        this.kafka = kafka;
        this.json = json;
    }

    @Bean
    NewTopic jobsTopic() {
        return TopicBuilder.name(JOBS_TOPIC).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic resultsTopic() {
        return TopicBuilder.name(RESULTS_TOPIC).partitions(1).replicas(1).build();
    }

    @Override
    public Optional<DetectionResult> submit(Request request) throws Exception {
        kafka.send(JOBS_TOPIC, request.id().toString(), json.writeValueAsString(request)).get(6, TimeUnit.SECONDS);
        return Optional.empty();
    }
}
