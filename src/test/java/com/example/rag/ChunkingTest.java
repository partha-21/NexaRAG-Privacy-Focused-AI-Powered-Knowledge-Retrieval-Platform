package com.example.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkingTest {

    private static final int MAX = 200;
    private static final int OVERLAP = 20;

    private static String longText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 60; i++) {
            sb.append("Sentence number ").append(i).append(" talks about topic ").append(i).append(". ");
        }
        return sb.toString();
    }

    @Test
    void longDocumentIsSplitIntoMultipleChunks() {
        List<TextSegment> segments =
                DocumentSplitters.recursive(MAX, OVERLAP).split(Document.from(longText()));
        assertThat(segments.size()).isGreaterThan(1);
    }

    @Test
    void noChunkExceedsMaxSize() {
        List<TextSegment> segments =
                DocumentSplitters.recursive(MAX, OVERLAP).split(Document.from(longText()));
        assertThat(segments).allSatisfy(s -> assertThat(s.text().length()).isLessThanOrEqualTo(MAX));
    }

    @Test
    void noContentIsLost() {
        String text = longText();
        List<TextSegment> segments =
                DocumentSplitters.recursive(MAX, OVERLAP).split(Document.from(text));
        String joined = String.join(" ", segments.stream().map(TextSegment::text).toList());
        // every numbered sentence must appear in at least one chunk
        for (int i = 1; i <= 60; i++) {
            assertThat(joined).contains("topic " + i + ".");
        }
    }

    @Test
    void shortDocumentStaysInOneChunk() {
        List<TextSegment> segments =
                DocumentSplitters.recursive(MAX, OVERLAP).split(Document.from("Just one short sentence."));
        assertThat(segments).hasSize(1);
    }
}
