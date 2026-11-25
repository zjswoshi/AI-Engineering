package com.example.starter.controller;

import com.example.starter.dto.CreateDetectionRequest;
import com.example.starter.dto.DetectionResponse;
import com.example.starter.exception.ApiError;
import com.example.starter.exception.ApiException;
import com.example.starter.model.DetectionJob;
import com.example.starter.service.DetectionService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DetectionController {
    private final DetectionService detection;

    DetectionController(DetectionService detection) {
        this.detection = detection;
    }

    @GetMapping("/api/detections")
    List<DetectionResponse> recent(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return detection.recent(authentication.getName(), admin).stream()
                .map(DetectionController::toResponse)
                .toList();
    }

    @PostMapping(path = "/api/detections", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DetectionResponse> submit(
            @RequestBody CreateDetectionRequest input, Authentication authentication) {
        String sample = input.sample();
        if (sample == null || sample.isBlank() || sample.codePointCount(0, sample.length()) > 120) {
            throw new ApiException(ApiError.INVALID_SAMPLE);
        }
        DetectionService.Submission submission;
        try {
            submission = detection.submit(sample.strip(), authentication.getName());
        } catch (Exception failure) {
            if (failure instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException(ApiError.DETECTION_TRANSPORT_UNAVAILABLE);
        }
        var status = submission.completedImmediately() ? HttpStatus.CREATED : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(toResponse(submission.job()));
    }

    private static DetectionResponse toResponse(DetectionJob job) {
        var result = job.result();
        var responseResult = result == null
                ? null
                : new DetectionResponse.Result(result.label(), result.score());
        return new DetectionResponse(job.id(), job.sample(), job.owner(), job.createdAt(), job.status(), responseResult);
    }
}
