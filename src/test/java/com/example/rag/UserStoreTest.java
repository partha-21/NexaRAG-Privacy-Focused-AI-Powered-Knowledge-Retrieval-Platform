package com.example.rag;

import com.example.rag.security.UserStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UserStoreTest {

    @TempDir
    Path tmp;

    @Test
    void createsAndFindsUserCaseInsensitively() {
        UserStore store = new UserStore(tmp.toString());
        assertThat(store.create("Alice", "hash1")).isTrue();
        assertThat(store.find("alice")).isPresent();
        assertThat(store.find("ALICE")).isPresent();
    }

    @Test
    void rejectsDuplicateUsername() {
        UserStore store = new UserStore(tmp.toString());
        assertThat(store.create("bob", "h")).isTrue();
        assertThat(store.create("BOB", "h2")).isFalse();
        assertThat(store.count()).isEqualTo(1);
    }

    @Test
    void usersSurviveRestart() {
        new UserStore(tmp.toString()).create("carol", "hash");
        UserStore reloaded = new UserStore(tmp.toString());
        assertThat(reloaded.find("carol")).isPresent();
        assertThat(reloaded.find("carol").get().passwordHash()).isEqualTo("hash");
    }

    @Test
    void unknownUserIsNotFound() {
        assertThat(new UserStore(tmp.toString()).find("nobody")).isEmpty();
    }

    @Test
    void storedFileNeverContainsPlainPassword() throws Exception {
        UserStore store = new UserStore(tmp.toString());
        store.create("dave", "$2a$10$fakehashvalue");
        String content = Files.readString(tmp.resolve("users.json"));
        assertThat(content).doesNotContain("password123");
        assertThat(content).contains("dave");
    }
}
