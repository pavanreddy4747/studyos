package com.studyos.app.controller;

import com.studyos.app.dto.SubmitAnswerRequest;
import com.studyos.app.dto.SubmitAnswerResponse;
import com.studyos.app.service.QuizAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = "*")
public class QuizController {

    @Autowired
    private QuizAttemptService quizAttemptService;

    @PostMapping("/submit")
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(@RequestBody SubmitAnswerRequest request) {
        QuizAttemptService.AttemptResult result =
                quizAttemptService.submitAnswer(request.getQuestionId(), request.getSubmittedAnswer());

        SubmitAnswerResponse response = new SubmitAnswerResponse(
                result.correct,
                result.question.getCorrectAnswer(),
                result.question.getExplanation()
        );
        return ResponseEntity.ok(response);
    }
}
