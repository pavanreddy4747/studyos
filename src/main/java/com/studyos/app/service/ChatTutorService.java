package com.studyos.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class ChatTutorService {

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

    private static final int MAX_CONTEXT_CHARS = 4000;

    public String answerQuestion(String sourceText, String summary, String studentQuestion) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY is not set.");
        }

        String context = sourceText.length() > MAX_CONTEXT_CHARS
                ? sourceText.substring(0, MAX_CONTEXT_CHARS)
                : sourceText;

        String prompt = """
            You are a friendly, encouraging study tutor helping a student understand their notes.
            Answer the student's question clearly and concisely (2-4 sentences unless more detail is
            truly needed). Base your answer on the study material below. If the question can't be
            answered from the material, say so honestly and give your best general knowledge answer,
            noting it's outside the provided notes.

            STUDY MATERIAL SUMMARY:
            %s

            STUDY MATERIAL (excerpt):
            %s

            STUDENT'S QUESTION:
            %s
            """.formatted(summary != null ? summary : "(no summary available)", context, studentQuestion);

        String requestBody = buildRequestBody(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) {
            throw new RuntimeException("The tutor is a bit busy right now — please try again in a few seconds.");
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("Chat tutor error (" + response.statusCode() + "): " + response.body());
        }

        return extractAnswer(response.body());
    }

    private String buildRequestBody(String prompt) throws Exception {
        var root = mapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.5);
        root.put("max_tokens", 512);

        var messages = root.putArray("messages");
        var userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);

        return mapper.writeValueAsString(root);
    }

    private String extractAnswer(String responseBody) throws Exception {
        JsonNode root = mapper.readTree(responseBody);
        JsonNode textNode = root.path("choices").path(0).path("message").path("content");
        if (textNode.isMissingNode()) {
            throw new RuntimeException("Unexpected response format from chat tutor.");
        }
        return textNode.asText();
    }
}