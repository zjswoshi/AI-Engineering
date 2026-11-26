package com.example.starter.controller;

import com.example.starter.exception.ApiError;
import com.example.starter.exception.ApiException;
import com.example.starter.model.BladeInspection;
import com.example.starter.service.BladeInspectionService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@Profile("blade & postgres")
@RequestMapping("/api/detection-tasks")
public class BladeInspectionController {
    private final BladeInspectionService inspections;

    public BladeInspectionController(BladeInspectionService inspections) {
        this.inspections = inspections;
    }

    @PostMapping(consumes = "application/zip")
    ResponseEntity<BladeInspection> submit(Authentication caller, @RequestParam String farmName,
            @RequestParam String turbineNumber, @RequestParam String filename, HttpServletRequest request) throws IOException {
        if (!"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) throw new ApiException(ApiError.ACCESS_DENIED);
        var task = inspections.submit(caller, farmName, turbineNumber, filename, request.getInputStream(), request.getContentLengthLong());
        return ResponseEntity.accepted().location(URI.create("/api/detection-tasks/" + task.id())).body(task);
    }

    @GetMapping
    BladeInspectionService.Page<BladeInspection> list(Authentication caller, @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return inspections.list(caller, page, size);
    }

    @GetMapping("/{id}")
    BladeInspection task(Authentication caller, @PathVariable UUID id) {
        return inspections.task(caller, id);
    }

    @GetMapping("/{id}/report")
    JsonNode report(Authentication caller, @PathVariable UUID id) throws IOException {
        return inspections.report(caller, id);
    }

    @GetMapping("/{id}/images")
    BladeInspectionService.Page<JsonNode> images(Authentication caller, @PathVariable UUID id,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) throws IOException {
        return inspections.images(caller, id, page, size);
    }

    @GetMapping("/{id}/images/{index}/preview")
    ResponseEntity<FileSystemResource> preview(Authentication caller, @PathVariable UUID id, @PathVariable int index) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_JPEG)
                .body(inspections.preview(caller, id, index));
    }

    @GetMapping("/{id}/report.pdf")
    ResponseEntity<FileSystemResource> pdf(Authentication caller, @PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=blade-report-" + id + ".pdf")
                .body(inspections.pdf(caller, id));
    }
}
