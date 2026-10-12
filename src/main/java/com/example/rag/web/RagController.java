package com.example.rag.web;

import com.example.rag.service.ChatService;
import com.example.rag.service.IndexService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RagController {

    public record AskRequest(String question) {}

    private final IndexService index;
    private final ChatService chat;

    public RagController(IndexService index, ChatService chat) {
        this.index = index;
        this.chat = chat;
    }

    @GetMapping("/documents")
    public List<String> documents() {
        return index.listFiles();
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String name = safeName(file.getOriginalFilename());
        if (name == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid file name"));
        }
        Files.copy(file.getInputStream(), index.docsDir().resolve(name), StandardCopyOption.REPLACE_EXISTING);
        index.refreshIfChanged();
        return ResponseEntity.ok(Map.of("uploaded", name, "documents", index.listFiles()));
    }

    @DeleteMapping("/documents/{name}")
    public ResponseEntity<?> delete(@PathVariable String name) throws IOException {
        String safe = safeName(name);
        if (safe == null || !Files.deleteIfExists(index.docsDir().resolve(safe))) {
            return ResponseEntity.notFound().build();
        }
        index.refreshIfChanged();
        return ResponseEntity.ok(Map.of("deleted", safe));
    }

    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody AskRequest req) {
        if (req == null || req.question() == null || req.question().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question is required"));
        }
        try {
            return ResponseEntity.ok(chat.ask(req.question().trim()));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Could not reach the local model. Is Ollama running and the model pulled?",
                    "detail", String.valueOf(e.getMessage())));
        }
    }

    /** Strips any path components so uploads cannot escape the docs folder. */
    private static String safeName(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String name = Path.of(raw).getFileName().toString();
        return (name.isBlank() || name.startsWith(".")) ? null : name;
    }
}
