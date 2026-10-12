package com.example.rag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Owns the vector index. Embeddings run in-process (ONNX), so nothing leaves the machine.
 *
 * Persistence: the index is serialized to data/index.json plus a manifest of file
 * name/size/modified-time. On startup, if the manifest still matches the docs folder,
 * the saved index is loaded instead of re-embedding everything.
 *
 * Trade-off: when any file changes, the whole index is rebuilt. That is simple and
 * always correct, and fast enough for personal-size document sets.
 */
@Service
public class IndexService {

    private static final Logger log = LoggerFactory.getLogger(IndexService.class);

    public record Match(String file, String text, double score) {}

    private final EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    private final Path docsDir;
    private final Path indexFile;
    private final Path manifestFile;
    private final int chunkSize;
    private final int chunkOverlap;

    private volatile InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
    private Map<String, String> lastManifest = Map.of();

    public IndexService(@Value("${rag.docs-dir}") String docsDir,
                        @Value("${rag.data-dir}") String dataDir,
                        @Value("${rag.chunk-size}") int chunkSize,
                        @Value("${rag.chunk-overlap}") int chunkOverlap) {
        this.docsDir = Path.of(docsDir);
        this.indexFile = Path.of(dataDir, "index.json");
        this.manifestFile = Path.of(dataDir, "manifest.properties");
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(docsDir);
        Files.createDirectories(manifestFile.getParent());
        Map<String, String> current = currentManifest();
        if (Files.exists(indexFile) && current.equals(readManifest())) {
            store = InMemoryEmbeddingStore.fromFile(indexFile);
            lastManifest = current;
            log.info("Loaded persisted index ({} files).", current.size());
        } else {
            refreshIfChanged();
        }
    }

    /** Re-indexes only if the docs folder differs from what was last indexed. */
    public synchronized boolean refreshIfChanged() {
        Map<String, String> current = currentManifest();
        if (current.equals(lastManifest)) {
            return false;
        }
        log.info("Documents changed, re-indexing {} file(s)...", current.size());
        indexDocuments(loadDocuments());
        store.serializeToFile(indexFile);
        writeManifest(current);
        lastManifest = current;
        log.info("Index updated.");
        return true;
    }

    /** Splits, embeds and stores the given documents, replacing the current index. */
    public void indexDocuments(List<Document> docs) {
        InMemoryEmbeddingStore<TextSegment> fresh = new InMemoryEmbeddingStore<>();
        if (!docs.isEmpty()) {
            EmbeddingStoreIngestor.builder()
                    .documentSplitter(DocumentSplitters.recursive(chunkSize, chunkOverlap))
                    .embeddingModel(embeddingModel)
                    .embeddingStore(fresh)
                    .build()
                    .ingest(docs);
        }
        this.store = fresh;
    }

    public List<Match> search(String question, int maxResults, double minScore) {
        Embedding query = embeddingModel.embed(question).content();
        var result = store.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(query)
                .maxResults(maxResults)
                .minScore(minScore)
                .build());
        return result.matches().stream()
                .map(m -> new Match(
                        m.embedded().metadata().getString("file_name"),
                        m.embedded().text(),
                        m.score()))
                .toList();
    }

    public List<String> listFiles() {
        return new ArrayList<>(currentManifest().keySet());
    }

    public Path docsDir() {
        return docsDir;
    }

    // ---- helpers -------------------------------------------------------------

    private List<Document> loadDocuments() {
        List<Document> docs = new ArrayList<>();
        for (Path p : listDocFiles()) {
            try {
                docs.add(FileSystemDocumentLoader.loadDocument(p, new ApacheTikaDocumentParser()));
            } catch (Exception e) {
                log.warn("Skipping {}: {}", p.getFileName(), e.getMessage());
            }
        }
        return docs;
    }

    private List<Path> listDocFiles() {
        try (Stream<Path> s = Files.list(docsDir)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private Map<String, String> currentManifest() {
        Map<String, String> m = new TreeMap<>();
        for (Path p : listDocFiles()) {
            try {
                m.put(p.getFileName().toString(),
                        Files.size(p) + ":" + Files.getLastModifiedTime(p).toMillis());
            } catch (IOException ignored) {
                // file vanished between listing and stat; next scan will catch it
            }
        }
        return m;
    }

    private Map<String, String> readManifest() {
        Map<String, String> m = new TreeMap<>();
        if (!Files.exists(manifestFile)) return m;
        try (InputStream in = Files.newInputStream(manifestFile)) {
            Properties props = new Properties();
            props.load(in);
            props.forEach((k, v) -> m.put(k.toString(), v.toString()));
        } catch (IOException e) {
            log.warn("Could not read manifest: {}", e.getMessage());
        }
        return m;
    }

    private void writeManifest(Map<String, String> manifest) {
        Properties props = new Properties();
        props.putAll(manifest);
        try (OutputStream out = Files.newOutputStream(manifestFile)) {
            props.store(out, "indexed documents");
        } catch (IOException e) {
            log.warn("Could not write manifest: {}", e.getMessage());
        }
    }
}
