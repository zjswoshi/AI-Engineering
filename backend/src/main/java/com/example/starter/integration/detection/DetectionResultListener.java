package com.example.starter.integration.detection;

import com.example.starter.integration.detection.DetectionTransport.Reply;
import com.example.starter.service.DetectionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "starter.detection.transport", havingValue = "kafka")
class DetectionResultListener {
    private final ObjectMapper json;
    private final DetectionService detection;

    DetectionResultListener(ObjectMapper json, DetectionService detection) {
        this.json = json;
        this.detection = detection;
    }

    @KafkaListener(topics = KafkaDetectionTransport.RESULTS_TOPIC, groupId = "detection-backend-v1")
    void accept(String response) {
        var reply = json.readValue(response, Reply.class);
        detection.recordResult(reply.id(), reply.result());
    }
}
