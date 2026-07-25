package com.studyos.app.service;

import com.studyos.app.model.QuizQuestion;
import com.studyos.app.repository.QuizQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuizAttemptService {

    @Autowired
    private QuizQuestionRepository questionRepository;

    @Autowired
    private SpacedRepetitionService spacedRepetitionService;

    public static class AttemptResult {
        public final boolean correct;
        public final QuizQuestion question;
        public AttemptResult(boolean correct, QuizQuestion question) {
            this.correct = correct;
            this.question = question;
        }
    }

    /**
     * Submits an answer for a question, updates mastery/streak stats via the
     * spaced repetition algorithm, and returns whether the answer was correct
     * along with the updated question (for explanation/correct-answer display).
     */
    public AttemptResult submitAnswer(Long questionId, String submittedAnswer) {
        QuizQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalStateException("Question not found"));

        boolean isCorrect = question.getCorrectAnswer() != null
                && question.getCorrectAnswer().trim().equalsIgnoreCase(submittedAnswer == null ? "" : submittedAnswer.trim());

        spacedRepetitionService.recordAnswer(question, isCorrect);
        questionRepository.save(question);

        return new AttemptResult(isCorrect, question);
    }
}
