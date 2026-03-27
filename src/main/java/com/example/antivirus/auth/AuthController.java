package com.example.antivirus.auth;

import com.example.antivirus.auth.dto.*;
import com.example.antivirus.security.JwtService;
import com.example.antivirus.user.Role;
import com.example.antivirus.user.User;
import com.example.antivirus.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequest req, Authentication authentication) {
        if (req.username() == null || req.username().isBlank()
                || req.password() == null || req.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username and password required");
        }
        if (users.existsByUsername(req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
        }

        boolean firstUser = users.count() == 0;
        if (!firstUser) {
            if (authentication == null || !isAdmin(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "admin only");
            }
        }

        Role role = resolveRole(req.role(), firstUser);

        User u = new User();
        u.setUsername(req.username().trim());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(role);
        u.setEnabled(true);
        u = users.save(u);

        return new RegisterResponse(u.getId(), u.getUsername(), u.getRole().name());
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private static Role resolveRole(String roleStr, boolean firstUser) {
        if (roleStr == null || roleStr.isBlank()) {
            return firstUser ? Role.ROLE_ADMIN : Role.ROLE_USER;
        }
        try {
            return Role.valueOf(roleStr.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role must be ROLE_USER or ROLE_ADMIN");
        }
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

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(refreshHash);
        rt.setExpiresAt(Instant.now().plusSeconds(refreshTtl));
        rt.setRevoked(false);
        refreshTokens.save(rt);

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