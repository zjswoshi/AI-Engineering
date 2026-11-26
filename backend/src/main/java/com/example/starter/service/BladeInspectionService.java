package com.example.starter.service;

import com.example.starter.exception.ApiError;
import com.example.starter.exception.ApiException;
import com.example.starter.model.BladeInspection;
import com.example.starter.repository.BladeInspectionRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@Profile("blade & postgres")
public class BladeInspectionService {
    private final BladeInspectionRepository tasks;
    private final BladeArchiveStorage storage;
    private final ObjectMapper json;

    public BladeInspectionService(BladeInspectionRepository tasks, BladeArchiveStorage storage, ObjectMapper json) {
        this.tasks = tasks;
        this.storage = storage;
        this.json = json;
    }

    public BladeInspection submit(Authentication caller, String farm, String turbine, String filename,
                                  InputStream upload, long length) throws IOException {
        farm = field(farm, 80);
        turbine = field(turbine, 60);
        filename = field(filename, 255);
        if (filename.contains("/") || filename.contains("\\")) throw new ApiException(ApiError.INVALID_REQUEST);
        UUID id = UUID.randomUUID();
        storage.upload(id, upload, length);
        return tasks.create(id, caller.getName(), farm, turbine, filename);
    }

    public Page<BladeInspection> list(Authentication caller, int page, int size) {
        pageCheck(page, size);
        var items = tasks.list(caller.getName(), admin(caller), (long) (page - 1) * size, size + 1);
        return new Page<>(items.subList(0, Math.min(size, items.size())), page, items.size() > size);
    }

    public BladeInspection task(Authentication caller, UUID id) {
        return visible(caller, id).task();
    }

    public JsonNode report(Authentication caller, UUID id) throws IOException {
        return readJson(published(caller, id).resolve("summary.json"));
    }

    public Page<JsonNode> images(Authentication caller, UUID id, int page, int size) throws IOException {
        pageCheck(page, size);
        Path report = published(caller, id);
        int total = readJson(report.resolve("summary.json")).get("imageCount").asInt();
        long offset = (long) (page - 1) * size;
        List<JsonNode> items = new ArrayList<>();
        for (long index = offset; index < Math.min(total, offset + size); index++) {
            items.add(readJson(report.resolve("image-" + index + ".json")));
        }
        return new Page<>(items, page, offset + size < total);
    }

    public FileSystemResource preview(Authentication caller, UUID id, int index) {
        return file(published(caller, id).resolve("image-" + index + ".jpg"));
    }

    public FileSystemResource pdf(Authentication caller, UUID id) {
        return file(published(caller, id).resolve("report.pdf"));
    }

    private JsonNode readJson(Path path) throws IOException {
        try (var input = Files.newInputStream(path)) {
            return json.readTree(input);
        }
    }

    private Path published(Authentication caller, UUID id) {
        var execution = visible(caller, id);
        if (!"COMPLETED".equals(execution.task().status())) throw new ApiException(ApiError.REPORT_PENDING);
        return storage.reportDirectory(execution.attemptId());
    }

    private BladeInspection.Execution visible(Authentication caller, UUID id) {
        return tasks.find(id, caller.getName(), admin(caller))
                .orElseThrow(() -> new ApiException(ApiError.INSPECTION_NOT_FOUND));
    }

    private static boolean admin(Authentication caller) {
        return caller.getAuthorities().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getAuthority()));
    }

    private static FileSystemResource file(Path path) {
        if (!Files.isRegularFile(path)) throw new ApiException(ApiError.INSPECTION_NOT_FOUND);
        return new FileSystemResource(path);
    }

    private static String field(String value, int limit) {
        String result = value.strip();
        if (result.isEmpty() || result.length() > limit || result.chars().anyMatch(Character::isISOControl)) {
            throw new ApiException(ApiError.INVALID_REQUEST);
        }
        return result;
    }

    private static void pageCheck(int page, int size) {
        if (page < 1 || size < 1 || size > 50) throw new ApiException(ApiError.INVALID_REQUEST);
    }

    public record Page<T>(List<T> items, int page, boolean hasNext) {}
}
