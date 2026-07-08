package com.roadcrack.service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    private final SecretKey secretKey;
    private final long expiration;
    private final long refreshExpiration;

    public JwtUtils(@Value("${jwt.secret:road-crack-detection-default-secret-key-please-change}") String secret,
                    @Value("${jwt.expiration:86400000}") long expiration,
                    @Value("${jwt.refresh-expiration:604800000}") long refreshExpiration) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            for (int index = keyBytes.length; index < 32; index++) {
                padded[index] = (byte) index;
            }
            keyBytes = padded;
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        return createToken(claims, expiration);
    }

    public String generateRefreshToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("refresh", true);
        return createToken(claims, refreshExpiration);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    public String getUsername(String token) {
        return parseClaims(token).get("username", String.class);
    }

    public boolean isRefreshToken(String token) {
        Object value = parseClaims(token).get("refresh");
        return value instanceof Boolean bool && bool;
    }

    private String createToken(Map<String, Object> claims, long expireMs) {
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMs))
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
