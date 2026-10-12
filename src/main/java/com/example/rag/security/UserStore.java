package com.example.rag.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores user accounts in data/users.json. Only BCrypt password hashes are stored,
 * never plain passwords. Usernames are case-insensitive (stored in lower case).
 */
@Service
public class UserStore {

    public record UserRecord(String username, String passwordHash, long createdAt) {}

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, UserRecord> users = new ConcurrentHashMap<>();

    public UserStore(@Value("${rag.data-dir}") String dataDir) {
        this.file = Path.of(dataDir, "users.json");
        load();
    }

    public static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    public Optional<UserRecord> find(String username) {
        return Optional.ofNullable(users.get(normalize(username)));
    }

    /** @return false if the username is already taken */
    public synchronized boolean create(String username, String passwordHash) {
        String name = normalize(username);
        if (users.containsKey(name)) return false;
        users.put(name, new UserRecord(name, passwordHash, System.currentTimeMillis()));
        save();
        return true;
    }

    public int count() {
        return users.size();
    }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            List<UserRecord> list = mapper.readValue(file.toFile(), new TypeReference<List<UserRecord>>() {});
            list.forEach(u -> users.put(u.username(), u));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("users.json.tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), new ArrayList<>(users.values()));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + file, e);
        }
    }
}
