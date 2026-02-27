package com.example.antivirus.auth;

import com.example.antivirus.auth.dto.*;
import com.example.antivirus.security.JwtService;
import com.example.antivirus.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    private final long accessTtl;
    private final long refreshTtl;

    public AuthController(
            UserRepository users,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder encoder,
            JwtService jwt,
            @Value("${security.jwt.access-ttl-seconds}") long accessTtl,
            @Value("${security.jwt.refresh-ttl-seconds}") long refreshTtl
    ) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest req) {
        var user = users.findByUsername(req.username())
                .orElseThrow(() -> new RuntimeException("Bad credentials"));

        if (!user.isEnabled() || !encoder.matches(req.password(), user.getPasswordHash())) {
            throw new RuntimeException("Bad credentials");
        }

        String access = jwt.generateAccess(user.getUsername(), user.getRole(), accessTtl);
        String refresh = jwt.generateRefresh(user.getUsername(), refreshTtl);

        // храним хэш refresh токена (а не сам токен)
        String refreshHash = sha256(refresh);

        refreshTokens.save(RefreshToken.builder()
                .user(user)
                .tokenHash(refreshHash)
                .expiresAt(Instant.now().plusSeconds(refreshTtl))
                .revoked(false)
                .build());

        return new TokenResponse(access, refresh);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody RefreshRequest req) {
        String refresh = req.refreshToken();
        String refreshHash = sha256(refresh);

        var tokenRow = refreshTokens.findByTokenHash(refreshHash)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (tokenRow.isRevoked() || tokenRow.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh token expired/revoked");
        }

        String username = jwt.getUsername(refresh);
        var user = users.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccess = jwt.generateAccess(user.getUsername(), user.getRole(), accessTtl);
        // refresh можно оставить тем же, или выпускать новый (безопаснее) — пока оставим тот же:
        return new TokenResponse(newAccess, refresh);
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}