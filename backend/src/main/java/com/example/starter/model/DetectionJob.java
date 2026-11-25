package com.example.starter.model;

import java.time.Instant;
import java.util.UUID;

public record DetectionJob(UUID id, String sample, String owner, Instant createdAt, String status, DetectionResult result) {}
