package com.studyos.app.service;

import com.studyos.app.model.QuizQuestion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implements a simplified spaced-repetition algorithm (inspired by SM-2 / Anki):
 * - Correct answer -> streak increases -> next review pushed further out
 * - Wrong answer -> streak resets to 0 -> review scheduled for tomorrow (short interval)
 * - After enough consecutive correct answers, the question is marked "mastered"
 */
@Service
public class SpacedRepetitionService {

    @Value("${studyos.spaced-repetition.intervals}")
    private String intervalsRaw;

    private List<Integer> intervals() {
        return Arrays.stream(intervalsRaw.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    private static final int MASTERY_STREAK_THRESHOLD = 5;

    public void recordAnswer(QuizQuestion question, boolean wasCorrect) {
        question.setTimesAnswered(question.getTimesAnswered() + 1);

        if (wasCorrect) {
            question.setTimesCorrect(question.getTimesCorrect() + 1);
            question.setCorrectStreak(question.getCorrectStreak() + 1);
        } else {
            question.setCorrectStreak(0);
        }

        if (question.getCorrectStreak() >= MASTERY_STREAK_THRESHOLD) {
            question.setMasteredFlag(true);
            question.setNextReviewDate(null);
            return;
        }

        List<Integer> schedule = intervals();
        int streakIndex = Math.min(question.getCorrectStreak(), schedule.size() - 1);
        int daysUntilNextReview = wasCorrect ? schedule.get(streakIndex) : 1;

        question.setNextReviewDate(LocalDate.now().plusDays(daysUntilNextReview));
    }
}
