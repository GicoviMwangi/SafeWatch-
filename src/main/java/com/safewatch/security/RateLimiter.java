package com.safewatch.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter extends OncePerRequestFilter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();
        String method = request.getMethod();

        if (!shouldLimit(method,path)){
            filterChain.doFilter(request,response);
            return;
        }

        String key = buildKey(request,path);
        Bucket bucket = buckets.computeIfAbsent(key + ":" + path, k -> newBucket(path));

        var probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()){
            response.setHeader("X-Rate-Limit-Remaining",String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request,response);
        }else {
            long waitSeconds = probe.getNanosToWaitForRefill();
            response.setStatus(429);
            response.setHeader("Retry After",String.valueOf(Math.max(1,waitSeconds)));
            response.getWriter().write("Too many requests, try again later");
        }
    }

    @SuppressWarnings("deprecation")
    private Bucket newBucket(String path) {
        Bandwidth limit;

        if (path.equals("/api/login")) {
            limit = Bandwidth.simple(5, Duration.ofMinutes(1));
        } else if (path.equals("/api/register")) {
            limit = Bandwidth.simple(3, Duration.ofMinutes(1));
        } else if (path.equals("/api/update/password")) {
            limit = Bandwidth.simple(3, Duration.ofMinutes(1));
        } else if (path.equals("/api/update/details")) {
            limit = Bandwidth.simple(3,Duration.ofMinutes(1));
        } else if (path.equals("/api/incident/report")) {
            limit = Bandwidth.simple(10, Duration.ofMinutes(1));
        }else {
            limit = Bandwidth.simple(30, Duration.ofMinutes(1));
        }
        return Bucket.builder().addLimit(limit).build();
    }

    private String buildKey(HttpServletRequest request, String path) {
        return clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if(xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private boolean shouldLimit(String method,String path) {
        return ("POST".equals(method) && (
                path.equals("/api/login") ||
                path.equals("/api/register") ||
                path.equals("/api/update/password")   ||
                path.equals("/api/update/details")  ||
                path.equals("/api/incident/report")
                ));
    }
}
