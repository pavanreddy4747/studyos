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

    private static final int MAX_SOURCE_CHARS = 6000;

    public GeneratedContent generateStudyMaterial(String sourceText, int questionCount) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "GROQ_API_KEY is not set. Set it as an environment variable before starting the app.");
        }

        String trimmedSource = sourceText.length() > MAX_SOURCE_CHARS
                ? sourceText.substring(0, MAX_SOURCE_CHARS)
                : sourceText;

        String prompt = buildPrompt(trimmedSource, questionCount);
        int maxTokens = Math.min(8192, 2048 + (questionCount * 220));
        String requestBody = buildRequestBody(prompt, maxTokens);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(90))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Groq API error (" + response.statusCode() + "): " + response.body());
        }

        String rawText = extractTextFromGroqResponse(response.body());
        String cleanJson = stripMarkdownFences(rawText);

        return mapper.readValue(cleanJson, GeneratedContent.class);
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
                "The AI response was cut off because it got too long. Try a lower question count or shorter notes.");
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