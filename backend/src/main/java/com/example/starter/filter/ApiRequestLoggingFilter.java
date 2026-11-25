package com.example.starter.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiRequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        response.setHeader("X-Request-ID", requestId);
        long started = System.nanoTime();
        boolean returned = false;
        try (var context = MDC.putCloseable("requestId", requestId)) {
            try {
                chain.doFilter(request, response);
                returned = true;
            } finally {
                int status = returned || response.isCommitted()
                        ? response.getStatus()
                        : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                logRequest(request, status, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
            }
        }
    }

    private static void logRequest(HttpServletRequest request, int status, long durationMs) {
        Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        Level level = Level.INFO;
        if (status >= 500) {
            level = Level.WARN;
        } else if (status < 400 && "/api/health".equals(route)) {
            level = Level.DEBUG;
        }
        LOG.atLevel(level)
                .addKeyValue("method", request.getMethod())
                .addKeyValue("route", route == null ? "unresolved" : route.toString())
                .addKeyValue("status", status)
                .addKeyValue("durationMs", durationMs)
                .log("http.request_completed");
    }
}
