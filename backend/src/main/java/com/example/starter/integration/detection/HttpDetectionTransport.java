package com.example.starter.integration.detection;

import com.example.starter.model.DetectionResult;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "starter.detection.transport", havingValue = "http")
class HttpDetectionTransport implements DetectionTransport {
    private final RestClient client;

    HttpDetectionTransport(RestClient.Builder builder, @Value("${starter.detection.algorithm-url}") String url) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = builder.baseUrl(url).requestFactory(factory).build();
    }

    @Override
    public Optional<DetectionResult> submit(Request request) {
        var reply = client.post().uri("/detect").body(request).retrieve().body(Reply.class);
        if (reply == null || !request.id().equals(reply.id()) || reply.result() == null) {
            throw new IllegalStateException("Invalid algorithm response");
        }
        return Optional.of(reply.result());
    }
}
