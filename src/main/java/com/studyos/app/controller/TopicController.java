package com.studyos.app.controller;

import com.studyos.app.dto.CreateTopicRequest;
import com.studyos.app.model.StudyTopic;
import com.studyos.app.service.StudyTopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/topics")
@CrossOrigin(origins = "*")
public class TopicController {

    @Autowired
    private StudyTopicService topicService;

    @GetMapping
    public List<StudyTopic> getAllTopics() {
        return topicService.getAllTopics();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudyTopic> getTopic(@PathVariable Long id) {
        return topicService.getTopicById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createTopic(@RequestBody CreateTopicRequest request) {
        if (request.getSourceText() == null || request.getSourceText().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sourceText is required"));
        }
        try {
            String title = (request.getTitle() == null || request.getTitle().isBlank())
                    ? "Untitled Topic" : request.getTitle();
            StudyTopic topic = topicService.createTopicFromMaterial(title, request.getSourceText());
            return ResponseEntity.status(HttpStatus.CREATED).body(topic);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate study material: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable Long id) {
        topicService.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }
}
