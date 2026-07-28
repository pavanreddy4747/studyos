package com.studyos.app.controller;

import com.studyos.app.dto.CreateTopicRequest;
import com.studyos.app.model.StudyTopic;
import com.studyos.app.service.PdfExtractionService;
import com.studyos.app.service.StudyTopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/topics")
@CrossOrigin(origins = "*")
public class TopicController {

    private static final Set<Integer> ALLOWED_QUESTION_COUNTS = Set.of(5, 10, 15, 20);

    @Autowired
    private StudyTopicService topicService;

    @Autowired
    private PdfExtractionService pdfExtractionService;

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

        int questionCount = request.getQuestionCount() != null ? request.getQuestionCount() : 5;
        if (!ALLOWED_QUESTION_COUNTS.contains(questionCount)) {
            return ResponseEntity.badRequest().body(Map.of("error", "questionCount must be 5, 10, 15, or 20"));
        }

        try {
            String title = (request.getTitle() == null || request.getTitle().isBlank())
                    ? "Untitled Topic" : request.getTitle();
            StudyTopic topic = topicService.createTopicFromMaterial(title, request.getSourceText(), questionCount);
            return ResponseEntity.status(HttpStatus.CREATED).body(topic);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate study material: " + e.getMessage()));
        }
    }

    @PostMapping("/extract-pdf")
    public ResponseEntity<?> extractPdfText(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded"));
        }
        if (!"application/pdf".equals(file.getContentType())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only PDF files are supported"));
        }
        try {
            String text = pdfExtractionService.extractText(file);
            return ResponseEntity.ok(Map.of("text", text));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to read PDF: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable Long id) {
        topicService.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }
}