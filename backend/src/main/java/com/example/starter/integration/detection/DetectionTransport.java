package com.example.starter.integration.detection;

import com.example.starter.model.DetectionResult;
import java.util.Optional;
import java.util.UUID;

public interface DetectionTransport {
    Optional<DetectionResult> submit(Request request) throws Exception;

    record Request(UUID id, String sample) {}

    record Reply(UUID id, DetectionResult result) {}
}
