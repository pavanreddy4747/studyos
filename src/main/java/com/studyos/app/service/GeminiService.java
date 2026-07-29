package com.studyos.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyos.app.dto.GeneratedContent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Service
public class GeminiService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.api.model}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    private static final int CHUNK_SIZE_CHARS = 4000;

    public GeneratedContent generateStudyMaterial(String sourceText, int questionCount) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "GROQ_API_KEY is not set. Set it as an environment variable before starting the app.");
        }

        List<String> chunks = splitIntoChunks(sourceText, CHUNK_SIZE_CHARS);
        List<Integer> questionsPerChunk = distributeQuestions(questionCount, chunks.size());

        List<GeneratedContent> partialResults = new java.util.ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            int qCount = questionsPerChunk.get(i);
            if (qCount == 0) continue;

            GeneratedContent partial = generateFromSingleChunkWithRetry(chunks.get(i), qCount);
            partialResults.add(partial);

            if (i < chunks.size() - 1) {
                Thread.sleep(3000);
            }
        }

        return mergeResults(partialResults);
    }

    private GeneratedContent generateFromSingleChunkWithRetry(String chunkText, int questionCount) throws Exception {
        int maxRetries = 3;
        int attempt = 0;

        while (true) {
            try {
                return generateFromSingleChunk(chunkText, questionCount);
            } catch (RateLimitException e) {
                attempt++;
                if (attempt > maxRetries) {
                    throw new RuntimeException(
                        "The AI service is currently rate-limited. Please wait a minute and try again.");
                }
                Thread.sleep(e.retryAfterMillis);
            }
        }
    }

    private List<String> splitIntoChunks(String text, int maxChunkSize) {
        List<String> chunks = new java.util.ArrayList<>();
        if (text.length() <= maxChunkSize) {
            chunks.add(text);
            return chunks;
        }

        String[] paragraphs = text.split("\n\n+");
        StringBuilder current = new StringBuilder();

        for (String para : paragraphs) {
            if (current.length() + para.length() + 2 > maxChunkSize && current.length() > 0) {
                chunks.add(current.toString());
                current = new StringBuilder();
            }
            if (para.length() > maxChunkSize) {
                for (int i = 0; i < para.length(); i += maxChunkSize) {
                    chunks.add(para.substring(i, Math.min(i + maxChunkSize, para.length())));
                }
            } else {
                if (current.length() > 0) current.append("\n\n");
                current.append(para);
            }
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }

        if (chunks.size() > 10) {
            List<String> merged = new java.util.ArrayList<>();
            int groupSize = (int) Math.ceil(chunks.size() / 10.0);
            for (int i = 0; i < chunks.size(); i += groupSize) {
                StringBuilder group = new StringBuilder();
                for (int j = i; j < Math.min(i + groupSize, chunks.size()); j++) {
                    if (group.length() > 0) group.append("\n\n");
                    group.append(chunks.get(j));
                }
                merged.add(group.toString());
            }
            return merged;
        }

        return chunks;
    }

    private List<Integer> distributeQuestions(int totalQuestions, int chunkCount) {
        List<Integer> result = new java.util.ArrayList<>();
        int base = totalQuestions / chunkCount;
        int remainder = totalQuestions % chunkCount;
        for (int i = 0; i < chunkCount; i++) {
            int count = base + (i < remainder ? 1 : 0);
            result.add(count);
        }
        return result;
    }

    private static class RateLimitException extends RuntimeException {
        final long retryAfterMillis;
        RateLimitException(long retryAfterMillis) {
            super("Rate limited, retry after " + retryAfterMillis + "ms");
            this.retryAfterMillis = retryAfterMillis;
        }
    }

    private GeneratedContent generateFromSingleChunk(String chunkText, int questionCount) throws Exception {
        String prompt = buildPrompt(chunkText, questionCount);
        int maxTokens = Math.min(4096, 1500 + (questionCount * 180));
        String requestBody = buildRequestBody(prompt, maxTokens);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(90))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) {
            long retryAfterMillis = parseRetryAfterMillis(response.body());
            throw new RateLimitException(retryAfterMillis);
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("Groq API error (" + response.statusCode() + "): " + response.body());
        }

        String rawText = extractTextFromGroqResponse(response.body());
        String cleanJson = stripMarkdownFences(rawText);

        return mapper.readValue(cleanJson, GeneratedContent.class);
    }

    private long parseRetryAfterMillis(String errorBody) {
        try {
            java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("try again in ([0-9.]+)s").matcher(errorBody);
            if (matcher.find()) {
                double seconds = Double.parseDouble(matcher.group(1));
                return (long) (seconds * 1000) + 500;
            }
        } catch (Exception ignored) {
        }
        return 10000;
    }

    private GeneratedContent mergeResults(List<GeneratedContent> partials) {
        GeneratedContent merged = new GeneratedContent();

        StringBuilder combinedSummary = new StringBuilder();
        List<GeneratedContent.GeneratedQuestion> allQuestions = new java.util.ArrayList<>();
        List<GeneratedContent.GeneratedFlashcard> allFlashcards = new java.util.ArrayList<>();

        for (GeneratedContent partial : partials) {
            if (partial.getSummary() != null && !partial.getSummary().isBlank()) {
                if (combinedSummary.length() > 0) combinedSummary.append(" ");
                combinedSummary.append(partial.getSummary());
            }
            if (partial.getQuestions() != null) allQuestions.addAll(partial.getQuestions());
            if (partial.getFlashcards() != null) allFlashcards.addAll(partial.getFlashcards());
        }

        merged.setSummary(combinedSummary.toString());
        merged.setQuestions(allQuestions);
        merged.setFlashcards(allFlashcards);
        return merged;
    }

    private String buildPrompt(String sourceText, int questionCount) {
        int flashcardCount = Math.max(6, questionCount / 2);
        int easyCount = questionCount / 3;
        int hardCount = questionCount / 3;
        int mediumCount = questionCount - easyCount - hardCount;

        return """
            You are an expert study assistant. Given the study material below, produce a JSON object
            (and ONLY a JSON object, no markdown fences, no extra commentary) with this exact structure:

            {
              "summary": "a concise 2-3 sentence summary of the material",
              "questions": [
                {
                  "question": "question text (max 25 words)",
                  "options": ["option A", "option B", "option C", "option D"],
                  "correctAnswer": "the exact text of the correct option",
                  "explanation": "max 15 words explaining why this is correct",
                  "difficulty": "EASY, MEDIUM, or HARD"
                }
              ],
              "flashcards": [
                { "front": "short term or question (max 12 words)", "back": "short definition or answer (max 20 words)" }
              ]
            }

            Generate exactly %d multiple-choice questions and exactly %d flashcards, based strictly on
            the material provided. Mix difficulty levels: roughly %d EASY (basic recall), %d MEDIUM
            (application/understanding), and %d HARD (analysis/synthesis) questions, and set the
            "difficulty" field accordingly for each question.

            Keep ALL text fields short and concise as instructed — brevity is critical.
            Keep questions clear and unambiguous with exactly 4 options each.
            Do not repeat questions or options. Cover different parts of the material, not just the beginning.

            STUDY MATERIAL:
            %s
            """.formatted(questionCount, flashcardCount, easyCount, mediumCount, hardCount, sourceText);
    }

    private String buildRequestBody(String prompt, int maxTokens) throws Exception {
        var root = mapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.4);
        root.put("max_tokens", maxTokens);

        var responseFormat = root.putObject("response_format");
        responseFormat.put("type", "json_object");

        var messages = root.putArray("messages");
        var userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);

        return mapper.writeValueAsString(root);
    }

    private String extractTextFromGroqResponse(String responseBody) throws Exception {
        JsonNode root = mapper.readTree(responseBody);
        JsonNode choice = root.path("choices").path(0);

        String finishReason = choice.path("finish_reason").asText("");
        if ("length".equals(finishReason)) {
            throw new RuntimeException(
                "The AI response was cut off because it got too long. Try a lower question count.");
        }

        JsonNode textNode = choice.path("message").path("content");
        if (textNode.isMissingNode()) {
            throw new RuntimeException("Unexpected Groq response format: " + responseBody);
        }
        return textNode.asText();
    }

    private String stripMarkdownFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(json)?", "").trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }
}