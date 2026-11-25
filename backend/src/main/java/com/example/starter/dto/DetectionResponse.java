package com.example.starter.dto;

import java.time.Instant;
import java.util.UUID;

public record DetectionResponse(
        UUID id,
        String sample,
        String owner,
        Instant createdAt,
        String status,
        Result result) {
    public record Result(String label, double score) {}
}
