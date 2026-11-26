package com.example.starter.integration.detection;

import com.example.starter.model.BladeInspection;
import com.example.starter.repository.BladeInspectionRepository;
import com.example.starter.service.BladeArchiveStorage;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
@EnableScheduling
@Profile("blade & postgres")
public class BladeInspectionWorker {
    private static final Logger LOG = LoggerFactory.getLogger(BladeInspectionWorker.class);
    private final BladeInspectionRepository tasks;
    private final BladeArchiveStorage storage;
    private final RestClient algorithm;
    private volatile boolean ready;

    public BladeInspectionWorker(BladeInspectionRepository tasks, BladeArchiveStorage storage,
            @Value("${BLADE_ALGORITHM_URL:http://127.0.0.1:8000}") String url,
            @Value("${BLADE_RUN_TIMEOUT_HOURS:12}") long timeoutHours) {
        this.tasks = tasks;
        this.storage = storage;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofHours(timeoutHours));
        this.algorithm = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        tasks.recoverInterrupted();
        ready = true;
    }

    @Scheduled(fixedDelay = 1500)
    public void dispatch() {
        if (ready) tasks.claim().ifPresent(this::run);
    }

    private void run(BladeInspection.Execution execution) {
        var task = execution.task();
        var attempt = execution.attemptId();
        String failure = null;
        try {
            algorithm.post().uri("/blade/inspect").contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("taskId", task.id(), "attemptId", attempt, "farmName", task.farmName(),
                            "turbineNumber", task.turbineNumber(), "originalName", task.originalName(),
                            "createdAt", task.createdAt().toString()))
                    .retrieve().toBodilessEntity();
            var report = storage.reportDirectory(attempt);
            if (!Files.isRegularFile(report.resolve("summary.json")) || !Files.isRegularFile(report.resolve("report.pdf"))) {
                throw new IllegalStateException("Algorithm did not publish the report");
            }
        } catch (HttpClientErrorException.UnprocessableEntity invalid) {
            LOG.warn("Invalid inspection archive taskId={}", task.id());
            failure = "ZIP 中的图片无法完整读取或超过处理限制，请检查后重新上传。";
        } catch (RuntimeException unavailable) {
            LOG.warn("Inspection failed taskId={} type={}", task.id(), unavailable.getClass().getSimpleName());
            failure = "检测或报告生成失败，请联系管理员并提供任务编号。";
        }
        tasks.finish(task.id(), attempt, failure);
    }
}
