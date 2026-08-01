package com.studyos.app.service;

import com.studyos.app.model.QuizQuestion;
import com.studyos.app.model.User;
import com.studyos.app.repository.QuizQuestionRepository;
import com.studyos.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class QuizAttemptService {

    @Autowired
    private QuizQuestionRepository questionRepository;

    @Autowired
    private SpacedRepetitionService spacedRepetitionService;

    @Autowired
    private UserRepository userRepository;

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
     * spaced repetition algorithm, updates the user's daily activity streak,
     * and returns whether the answer was correct along with the updated question.
     */
    public AttemptResult submitAnswer(Long questionId, String submittedAnswer, User user) {
        QuizQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalStateException("Question not found"));

        boolean isCorrect = question.getCorrectAnswer() != null
                && question.getCorrectAnswer().trim().equalsIgnoreCase(submittedAnswer == null ? "" : submittedAnswer.trim());

        spacedRepetitionService.recordAnswer(question, isCorrect);
        questionRepository.save(question);

        if (user != null) {
            updateStreak(user);
        }

        return new AttemptResult(isCorrect, question);
    }

    /**
     * Updates the user's daily activity streak. If they already answered something today,
     * streak stays the same. If yesterday was their last activity, streak increments.
     * If there's a gap of more than a day, streak resets to 1.
     */
    private void updateStreak(User user) {
        String today = LocalDate.now().toString();
        String lastActivity = user.getLastActivityDate();

        if (today.equals(lastActivity)) {
            return; // already counted today
        }

        if (lastActivity != null) {
            LocalDate last = LocalDate.parse(lastActivity);
            if (last.equals(LocalDate.now().minusDays(1))) {
                user.setCurrentStreak(user.getCurrentStreak() + 1);
            } else {
                user.setCurrentStreak(1);
            }
        } else {
            user.setCurrentStreak(1);
        }

        user.setLastActivityDate(today);
        userRepository.save(user);
    }
}
