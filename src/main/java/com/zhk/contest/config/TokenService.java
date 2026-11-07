package com.zhk.contest.config;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    private final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();

    public String createToken(Long userId, String role) {
        String token = UUID.randomUUID().toString().replace("-", "");
        boolean isAdmin = role != null && (role.endsWith("_ADMIN") || "ADMIN".equals(role) || "SUPER_ADMIN".equals(role));
        tokens.put(token, new TokenInfo(
                isAdmin ? userId : null,   // adminUserId — set only for admins
                userId,                    // userId — always set so non-admin endpoints work for admins too
                role));
        return token;
    }

    public TokenInfo getTokenInfo(String token) {
        return tokens.get(token);
    }

    public void removeToken(String token) {
        tokens.remove(token);
    }

    public record TokenInfo(Long adminUserId, Long userId, String role) {}
}
