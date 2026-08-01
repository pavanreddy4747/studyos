package com.studyos.app.controller;

import com.studyos.app.dto.SubmitAnswerRequest;
import com.studyos.app.dto.SubmitAnswerResponse;
import com.studyos.app.model.User;
import com.studyos.app.service.CurrentUserService;
import com.studyos.app.service.QuizAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = "*")
public class QuizController {

    @Autowired
    private QuizAttemptService quizAttemptService;

    @Autowired
    private CurrentUserService currentUserService;

    @PostMapping("/submit")
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(@RequestBody SubmitAnswerRequest request, Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);

        QuizAttemptService.AttemptResult result =
                quizAttemptService.submitAnswer(request.getQuestionId(), request.getSubmittedAnswer(), user);

        SubmitAnswerResponse response = new SubmitAnswerResponse(
                result.correct,
                result.question.getCorrectAnswer(),
                result.question.getExplanation()
        );
        return ResponseEntity.ok(response);
    }
}
