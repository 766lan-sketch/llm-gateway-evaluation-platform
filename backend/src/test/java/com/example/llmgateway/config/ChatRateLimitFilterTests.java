package com.example.llmgateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ChatRateLimitFilterTests {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void rejectsRequestsAfterConfiguredLimit() throws Exception {
        ChatRateLimitFilter filter = new ChatRateLimitFilter(2, 60, FIXED_CLOCK);
        AtomicInteger executedRequests = new AtomicInteger();

        MockHttpServletResponse first = execute(filter, executedRequests);
        MockHttpServletResponse second = execute(filter, executedRequests);
        MockHttpServletResponse third = execute(filter, executedRequests);

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(third.getStatus()).isEqualTo(429);
        assertThat(third.getHeader("Retry-After")).isEqualTo("60");
        assertThat(executedRequests).hasValue(2);
    }

    private MockHttpServletResponse execute(ChatRateLimitFilter filter, AtomicInteger executedRequests)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/models/chat");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> executedRequests.incrementAndGet());
        return response;
    }
}
