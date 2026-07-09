package com.roadcrack.bootstrap.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roadcrack.service.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtAuthenticationFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtUtils jwtUtils = new JwtUtils("road-crack-test-secret-key-1234567890", 60_000L, 120_000L);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtils, objectMapper);

    @Test
    void shouldAllowWhitelistedRequestWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNotNull(filterChain.getRequest());
        assertNull(request.getAttribute("userId"));
    }

    @Test
    void shouldRejectProtectedRequestWhenTokenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/current");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(401, response.getStatus());
        assertEquals(401, body.path("code").asInt());
        assertEquals("missing token", body.path("message").asText());
        assertNull(filterChain.getRequest());
    }

    @Test
    void shouldRejectProtectedRequestWhenTokenInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/role/list");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(401, response.getStatus());
        assertEquals(401, body.path("code").asInt());
        assertEquals("invalid token", body.path("message").asText());
        assertNull(filterChain.getRequest());
    }

    @Test
    void shouldPopulateUserAttributesWhenBearerTokenIsValid() throws Exception {
        String token = jwtUtils.generateToken(21L, "zhangsan");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/department/list");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNotNull(filterChain.getRequest());
        assertEquals(21L, request.getAttribute("userId"));
        assertEquals("zhangsan", request.getAttribute("username"));
    }

    @Test
    void shouldAcceptTokenHeaderFallback() throws Exception {
        String token = jwtUtils.generateToken(25L, "lisi");
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/auth/change-password");
        request.addHeader("Token", token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNotNull(filterChain.getRequest());
        assertEquals(25L, request.getAttribute("userId"));
        assertEquals("lisi", request.getAttribute("username"));
    }
}
