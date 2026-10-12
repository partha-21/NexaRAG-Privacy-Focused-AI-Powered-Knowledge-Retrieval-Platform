package com.example.rag;

import com.example.rag.service.IndexService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the right document comes back first for clearly different topics.
 * Uses the real in-process ONNX embedding model, so no network or Ollama is needed.
 */
class RetrievalQualityTest {

    @TempDir
    Path tmp;

    private IndexService index;

    @BeforeEach
    void setUp() {
        index = new IndexService(tmp.resolve("docs").toString(), tmp.resolve("data").toString(), 300, 30);
        index.indexDocuments(List.of(
                doc("geography.txt",
                        "Paris is the capital city of France. It sits on the river Seine and is known for the Eiffel Tower."),
                doc("biology.txt",
                        "Photosynthesis is the process by which plants use sunlight, water and carbon dioxide to make glucose and oxygen."),
                doc("programming.txt",
                        "Java is a statically typed programming language. The JVM runs compiled bytecode and manages memory with a garbage collector.")
        ));
    }

    private static Document doc(String fileName, String text) {
        return Document.from(text, Metadata.from("file_name", fileName));
    }

    @Test
    void capitalQuestionFindsGeographyDocument() {
        var matches = index.search("What is the capital of France?", 3, 0.0);
        assertThat(matches).isNotEmpty();
        assertThat(matches.get(0).file()).isEqualTo("geography.txt");
    }

    @Test
    void plantQuestionFindsBiologyDocument() {
        var matches = index.search("How do plants turn sunlight into energy?", 3, 0.0);
        assertThat(matches.get(0).file()).isEqualTo("biology.txt");
    }

    @Test
    void codingQuestionFindsProgrammingDocument() {
        var matches = index.search("How does the JVM handle memory?", 3, 0.0);
        assertThat(matches.get(0).file()).isEqualTo("programming.txt");
    }

    @Test
    void minScoreFiltersOutIrrelevantResults() {
        var matches = index.search("What is the capital of France?", 3, 0.99);
        assertThat(matches).isEmpty();
    }
}
