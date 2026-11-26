package com.example.starter.service;

import com.example.starter.exception.ApiError;
import com.example.starter.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("blade & postgres")
public class BladeArchiveStorage {
    private final Path uploads;
    private final Path reports;
    private final long limit;
    private final long expandedLimit;

    public BladeArchiveStorage(@Value("${BLADE_DATA_DIR:./blade-data}") String root,
                               @Value("${BLADE_MAX_UPLOAD_BYTES:10000000000}") long limit,
                               @Value("${BLADE_MAX_EXPANDED_BYTES:30000000000}") long expandedLimit) {
        this.uploads = Path.of(root).resolve("uploads");
        this.reports = Path.of(root).resolve("reports");
        this.limit = limit;
        this.expandedLimit = expandedLimit;
    }

    public Path upload(UUID id, InputStream input, long declaredLength) throws IOException {
        if (declaredLength > limit) throw new ApiException(ApiError.ARCHIVE_TOO_LARGE);
        if (declaredLength == 0) throw new ApiException(ApiError.INVALID_ARCHIVE);
        Files.createDirectories(uploads);
        if (declaredLength > Files.getFileStore(uploads).getUsableSpace()) {
            throw new ApiException(ApiError.UPLOAD_STORAGE_FULL);
        }
        Path temporary = Files.createTempFile(uploads, id + "-", ".part");
        Path archive = uploads.resolve(id + ".zip");
        try {
            long written = 0;
            try (var output = Files.newOutputStream(temporary)) {
                byte[] buffer = new byte[128 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    written += count;
                    if (written > limit) throw new ApiException(ApiError.ARCHIVE_TOO_LARGE);
                    output.write(buffer, 0, count);
                }
            }
            if (declaredLength >= 0 && written != declaredLength) throw new ApiException(ApiError.INVALID_ARCHIVE);
            checkZip(temporary);
            return Files.move(temporary, archive, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void checkZip(Path file) throws IOException {
        try (var zip = new ZipFile(file.toFile(), Charset.forName("GB18030"))) {
            if (zip.size() > 100_000) throw new ApiException(ApiError.INVALID_ARCHIVE);
            boolean hasImage = false;
            long total = 0;
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory() || entry.getName().startsWith("__MACOSX/")) continue;
                if (entry.getSize() < 0 || entry.getSize() > expandedLimit - total) {
                    throw new ApiException(ApiError.INVALID_ARCHIVE);
                }
                total += entry.getSize();
                String name = entry.getName().toLowerCase(Locale.ROOT);
                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
                    if (entry.getSize() == 0 || entry.getSize() > 100_000_000) {
                        throw new ApiException(ApiError.INVALID_ARCHIVE);
                    }
                    hasImage = true;
                }
            }
            if (!hasImage) throw new ApiException(ApiError.INVALID_ARCHIVE);
        } catch (ZipException failure) {
            throw new ApiException(ApiError.INVALID_ARCHIVE);
        }
    }

    public Path reportDirectory(UUID attemptId) {
        return reports.resolve(attemptId.toString());
    }
}
