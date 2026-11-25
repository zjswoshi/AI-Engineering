package com.example.starter.repository;

import com.example.starter.model.DetectionJob;
import com.example.starter.model.DetectionResult;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryDetectionRepository {
    private final ConcurrentHashMap<UUID, DetectionJob> jobs = new ConcurrentHashMap<>();

    public void save(DetectionJob job) {
        jobs.put(job.id(), job);
    }

    public boolean completePending(UUID id, DetectionResult result) {
        var job = jobs.get(id);
        if (job == null || !"PENDING".equals(job.status())) {
            return false;
        }
        var completed = new DetectionJob(id, job.sample(), job.owner(), job.createdAt(), "COMPLETED", result);
        return jobs.replace(id, job, completed);
    }

    public void deleteById(UUID id) {
        jobs.remove(id);
    }

    public DetectionJob findById(UUID id) {
        return jobs.get(id);
    }

    public List<DetectionJob> findRecentVisibleTo(String username, boolean admin) {
        return jobs.values().stream()
                .filter(job -> admin || job.owner().equals(username))
                .sorted(Comparator.comparing(DetectionJob::createdAt).reversed())
                .limit(20)
                .toList();
    }
}
