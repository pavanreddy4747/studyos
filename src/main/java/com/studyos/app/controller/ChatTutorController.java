package com.studyos.app.controller;

import com.studyos.app.dto.ChatTutorRequest;
import com.studyos.app.model.StudyTopic;
import com.studyos.app.service.ChatTutorService;
import com.studyos.app.service.StudyTopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatTutorController {

    @Autowired
    private ChatTutorService chatTutorService;

    @Autowired
    private StudyTopicService topicService;

    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody ChatTutorRequest request) {
        if (request.getTopicId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "topicId is required"));
        }
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "question is required"));
        }

        Optional<StudyTopic> topicOpt = topicService.getTopicById(request.getTopicId());
        if (topicOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Topic not found"));
        }

        StudyTopic topic = topicOpt.get();

        try {
            String answer = chatTutorService.answerQuestion(
                    topic.getSourceText(), topic.getSummary(), request.getQuestion());
            return ResponseEntity.ok(Map.of("answer", answer));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get an answer: " + e.getMessage()));
        }
    }
}