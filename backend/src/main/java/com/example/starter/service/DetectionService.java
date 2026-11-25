package com.example.starter.service;

import com.example.starter.integration.detection.DetectionTransport;
import com.example.starter.integration.detection.DetectionTransport.Request;
import com.example.starter.model.DetectionJob;
import com.example.starter.model.DetectionResult;
import com.example.starter.repository.InMemoryDetectionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DetectionService {
    private static final Logger LOG = LoggerFactory.getLogger(DetectionService.class);

    private final InMemoryDetectionRepository jobs;
    private final DetectionTransport transport;

    DetectionService(InMemoryDetectionRepository jobs, DetectionTransport transport) {
        this.jobs = jobs;
        this.transport = transport;
    }

    public List<DetectionJob> recent(String username, boolean admin) {
        return jobs.findRecentVisibleTo(username, admin);
    }

    public Submission submit(String sample, String owner) throws Exception {
        var job = new DetectionJob(UUID.randomUUID(), sample, owner, Instant.now(), "PENDING", null);
        jobs.save(job);
        LOG.atInfo().addKeyValue("taskId", job.id()).log("detection.task_created");
        try {
            var result = transport.submit(new Request(job.id(), job.sample()));
            result.ifPresent(value -> recordResult(job.id(), value));
            return new Submission(jobs.findById(job.id()), result.isPresent());
        } catch (Exception failure) {
            jobs.deleteById(job.id());
            LOG.atWarn()
                    .addKeyValue("taskId", job.id())
                    .addKeyValue("errorType", failure.getClass().getSimpleName())
                    .log("detection.dispatch_failed");
            throw failure;
        }
    }

    public void recordResult(UUID id, DetectionResult result) {
        if (id == null || result == null) {
            throw new IllegalArgumentException("Invalid algorithm reply");
        }
        if (jobs.completePending(id, result)) {
            LOG.atInfo().addKeyValue("taskId", id).log("detection.task_completed");
        }
    }

    public record Submission(DetectionJob job, boolean completedImmediately) {}
}
