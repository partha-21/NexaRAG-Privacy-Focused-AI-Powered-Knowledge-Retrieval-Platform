package com.example.rag.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    public record Source(String file, String excerpt, double score) {}

    public record Answer(String answer, List<Source> sources) {}

    private static final int MAX_CHUNKS = 4;
    private static final double MIN_SCORE = 0.4;

    private final IndexService index;
    private final ChatLanguageModel llm;

    public ChatService(IndexService index, ChatLanguageModel llm) {
        this.index = index;
        this.llm = llm;
    }

    public Answer ask(String question) {
        List<IndexService.Match> matches = index.search(question, MAX_CHUNKS, MIN_SCORE);
        if (matches.isEmpty()) {
            return new Answer("I couldn't find anything relevant in your documents.", List.of());
        }

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < matches.size(); i++) {
            IndexService.Match m = matches.get(i);
            context.append("[").append(i + 1).append("] (").append(m.file()).append(")\n")
                    .append(m.text()).append("\n\n");
        }

        String prompt = """
                Answer the question using ONLY the context below.
                If the answer is not in the context, say you don't know.
                Mention the source number like [1] when you use a passage.

                Context:
                %s
                Question: %s
                """.formatted(context, question);

        String answer = llm.chat(prompt);

        List<Source> sources = matches.stream()
                .map(m -> new Source(m.file(), excerpt(m.text()), Math.round(m.score() * 100) / 100.0))
                .toList();
        return new Answer(answer, sources);
    }

    private static String excerpt(String text) {
        String t = text.replaceAll("\\s+", " ").trim();
        return t.length() <= 220 ? t : t.substring(0, 220) + "...";
    }
}
