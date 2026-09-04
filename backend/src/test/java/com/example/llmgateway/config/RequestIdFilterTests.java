package com.example.llmgateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTests {
    @Test
    void keepsIncomingRequestIdForCrossServiceTracing() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/models");
        request.addHeader(RequestIdFilter.HEADER_NAME, "trace-from-client");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo("trace-from-client");
    }
}
