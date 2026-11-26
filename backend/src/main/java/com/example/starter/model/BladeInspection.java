package com.example.starter.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BladeInspection(UUID id, String farmName, String turbineNumber, String originalName,
                              String status, OffsetDateTime createdAt, String failureMessage) {
    public record Execution(BladeInspection task, UUID attemptId) {}
}
