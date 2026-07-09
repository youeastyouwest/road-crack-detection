package com.roadcrack.bootstrap.security;

import com.roadcrack.service.security.JwtUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    @Test
    void shouldGenerateAndParseAccessToken() {
        JwtUtils jwtUtils = new JwtUtils("road-crack-test-secret-key-1234567890", 60_000L, 120_000L);

        String token = jwtUtils.generateToken(7L, "tester");

        assertNotNull(token);
        assertTrue(jwtUtils.validateToken(token));
        assertEquals(7L, jwtUtils.getUserId(token));
        assertEquals("tester", jwtUtils.getUsername(token));
        assertFalse(jwtUtils.isRefreshToken(token));
    }

    @Test
    void shouldGenerateRefreshToken() {
        JwtUtils jwtUtils = new JwtUtils("road-crack-test-secret-key-1234567890", 60_000L, 120_000L);

        String refreshToken = jwtUtils.generateRefreshToken(11L, "refresh-user");

        assertNotNull(refreshToken);
        assertTrue(jwtUtils.validateToken(refreshToken));
        assertEquals(11L, jwtUtils.getUserId(refreshToken));
        assertEquals("refresh-user", jwtUtils.getUsername(refreshToken));
        assertTrue(jwtUtils.isRefreshToken(refreshToken));
    }

    @Test
    void shouldRejectTamperedToken() {
        JwtUtils jwtUtils = new JwtUtils("road-crack-test-secret-key-1234567890", 60_000L, 120_000L);

        String token = jwtUtils.generateToken(3L, "tamper-check");
        String tamperedToken = token.substring(0, token.length() - 2) + "ab";

        assertFalse(jwtUtils.validateToken(tamperedToken));
    }
}
