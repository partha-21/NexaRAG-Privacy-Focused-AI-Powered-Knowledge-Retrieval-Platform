package com.example.rag.web;

import com.example.rag.security.UserStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Registration and "who am I". Login and logout are handled by Spring Security
 * at POST /api/auth/login and POST /api/auth/logout (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record RegisterRequest(String username, String password) {}

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_.-]{3,32}$");
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 64;

    private final UserStore users;
    private final PasswordEncoder encoder;
    private final boolean allowRegistration;

    public AuthController(UserStore users, PasswordEncoder encoder,
                          @Value("${rag.allow-registration:true}") boolean allowRegistration) {
        this.users = users;
        this.encoder = encoder;
        this.allowRegistration = allowRegistration;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (!allowRegistration) {
            return ResponseEntity.status(403).body(Map.of("error", "Registration is disabled"));
        }
        if (req == null || req.username() == null || !USERNAME.matcher(req.username().trim()).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Username must be 3-32 characters: letters, numbers, . _ -"));
        }
        String pw = req.password();
        if (pw == null || pw.length() < MIN_PASSWORD || pw.length() > MAX_PASSWORD) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Password must be " + MIN_PASSWORD + "-" + MAX_PASSWORD + " characters"));
        }
        if (!users.create(req.username(), encoder.encode(pw))) {
            return ResponseEntity.status(409).body(Map.of("error", "That username is already taken"));
        }
        return ResponseEntity.status(201).body(Map.of("username", UserStore.normalize(req.username())));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not signed in"));
        }
        return ResponseEntity.ok(Map.of("username", auth.getName()));
    }
}
