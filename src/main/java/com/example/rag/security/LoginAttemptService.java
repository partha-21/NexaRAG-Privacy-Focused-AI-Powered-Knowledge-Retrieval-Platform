package com.example.rag.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Locks a username for a few minutes after repeated wrong passwords (basic brute-force protection). */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_TIME = Duration.ofMinutes(5);

    private record State(int failures, Instant lockedUntil) {}

    private final Map<String, State> states = new ConcurrentHashMap<>();

    public boolean isLocked(String username) {
        State s = states.get(UserStore.normalize(username));
        return s != null && s.lockedUntil() != null && Instant.now().isBefore(s.lockedUntil());
    }

    public void recordFailure(String username) {
        if (states.size() > 10_000) states.clear(); // keeps memory bounded
        states.compute(UserStore.normalize(username), (k, old) -> {
            int failures = (old == null ? 0 : old.failures()) + 1;
            return failures >= MAX_FAILURES
                    ? new State(0, Instant.now().plus(LOCK_TIME))
                    : new State(failures, null);
        });
    }

    public void recordSuccess(String username) {
        states.remove(UserStore.normalize(username));
    }

    @EventListener
    public void onBadCredentials(AuthenticationFailureBadCredentialsEvent e) {
        recordFailure(e.getAuthentication().getName());
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent e) {
        recordSuccess(e.getAuthentication().getName());
    }
}
