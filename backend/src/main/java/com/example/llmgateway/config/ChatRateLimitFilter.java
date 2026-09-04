package com.example.llmgateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 对聊天接口执行固定时间窗口限流，避免恶意请求快速消耗模型额度。
 * 学习项目使用单机内存实现；生产环境可替换为 Redis 分布式限流。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ChatRateLimitFilter extends OncePerRequestFilter {
    private final int maxRequests;
    private final long windowMillis;
    private final Clock clock;
    private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Autowired
    public ChatRateLimitFilter(
            @Value("${gateway.rate-limit.max-requests:20}") int maxRequests,
            @Value("${gateway.rate-limit.window-seconds:60}") long windowSeconds) {
        this(maxRequests, windowSeconds, Clock.systemUTC());
    }

    ChatRateLimitFilter(int maxRequests, long windowSeconds, Clock clock) {
        if (maxRequests < 1 || windowSeconds < 1) {
            throw new IllegalArgumentException("限流参数必须大于 0");
        }
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !"/api/models/chat".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long now = clock.millis();
        WindowCounter counter = counters.computeIfAbsent(request.getRemoteAddr(), ignored -> new WindowCounter(now));
        LimitDecision decision = counter.acquire(now, windowMillis, maxRequests);

        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        if (!decision.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"请求过于频繁，请稍后重试\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static final class WindowCounter {
        private long windowStartedAt;
        private int count;

        private WindowCounter(long windowStartedAt) {
            this.windowStartedAt = windowStartedAt;
        }

        private synchronized LimitDecision acquire(long now, long windowMillis, int maxRequests) {
            if (now - windowStartedAt >= windowMillis) {
                windowStartedAt = now;
                count = 0;
            }

            if (count >= maxRequests) {
                long remainingMillis = Math.max(1, windowMillis - (now - windowStartedAt));
                long retryAfterSeconds = Math.max(1, (remainingMillis + 999) / 1000);
                return new LimitDecision(false, 0, retryAfterSeconds);
            }

            count++;
            return new LimitDecision(true, maxRequests - count, 0);
        }
    }

    private record LimitDecision(boolean allowed, int remaining, long retryAfterSeconds) {}
}
