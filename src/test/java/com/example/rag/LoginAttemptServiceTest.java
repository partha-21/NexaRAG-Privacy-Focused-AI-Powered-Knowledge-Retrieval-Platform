package com.example.rag;

import com.example.rag.security.LoginAttemptService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    @Test
    void locksAfterFiveFailures() {
        LoginAttemptService svc = new LoginAttemptService();
        for (int i = 0; i < 4; i++) svc.recordFailure("eve");
        assertThat(svc.isLocked("eve")).isFalse();
        svc.recordFailure("eve");
        assertThat(svc.isLocked("eve")).isTrue();
    }

    @Test
    void successResetsFailureCount() {
        LoginAttemptService svc = new LoginAttemptService();
        for (int i = 0; i < 4; i++) svc.recordFailure("frank");
        svc.recordSuccess("frank");
        svc.recordFailure("frank");
        assertThat(svc.isLocked("frank")).isFalse();
    }

    @Test
    void lockIsPerUserAndCaseInsensitive() {
        LoginAttemptService svc = new LoginAttemptService();
        for (int i = 0; i < 5; i++) svc.recordFailure("Grace");
        assertThat(svc.isLocked("grace")).isTrue();
        assertThat(svc.isLocked("heidi")).isFalse();
    }
}
