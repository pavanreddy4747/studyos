package com.studyos.app.service;

import com.studyos.app.dto.GeneratedContent;
import com.studyos.app.model.Flashcard;
import com.studyos.app.model.QuizQuestion;
import com.studyos.app.model.StudyTopic;
import com.studyos.app.model.User;
import com.studyos.app.repository.FlashcardRepository;
import com.studyos.app.repository.QuizQuestionRepository;
import com.studyos.app.repository.StudyTopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class StudyTopicService {

    @Autowired
    private StudyTopicRepository topicRepository;

    @Autowired
    private QuizQuestionRepository questionRepository;

    @Autowired
    private FlashcardRepository flashcardRepository;

    @Autowired
    private GeminiService geminiService;

    public StudyTopic createTopicFromMaterial(User owner, String title, String sourceText, int questionCount) throws Exception {
        StudyTopic topic = new StudyTopic(title, sourceText);
        topic.setOwner(owner);
        topic = topicRepository.save(topic);

        GeneratedContent generated = geminiService.generateStudyMaterial(sourceText, questionCount);

        topic.setSummary(generated.getSummary());

        for (GeneratedContent.GeneratedQuestion gq : generated.getQuestions()) {
            QuizQuestion q = new QuizQuestion();
            q.setTopic(topic);
            q.setQuestionText(gq.getQuestion());
            q.setOptionsRaw(String.join("||", gq.getOptions()));
            q.setCorrectAnswer(gq.getCorrectAnswer());
            q.setExplanation(gq.getExplanation());
            q.setDifficulty(gq.getDifficulty() != null ? gq.getDifficulty().toUpperCase() : "MEDIUM");
            q.setNextReviewDate(LocalDate.now());
            questionRepository.save(q);
            topic.getQuestions().add(q);
        }

        for (GeneratedContent.GeneratedFlashcard gf : generated.getFlashcards()) {
            Flashcard card = new Flashcard(topic, gf.getFront(), gf.getBack());
            flashcardRepository.save(card);
            topic.getFlashcards().add(card);
        }

        return topicRepository.save(topic);
    }

    public List<StudyTopic> getTopicsForOwner(Long ownerId) {
        return topicRepository.findByOwnerId(ownerId);
    }

    public Optional<StudyTopic> getTopicByIdForOwner(Long id, Long ownerId) {
        return topicRepository.findById(id)
                .filter(t -> t.getOwner() != null && t.getOwner().getId().equals(ownerId));
    }

    public void deleteTopicForOwner(Long id, Long ownerId) {
        getTopicByIdForOwner(id, ownerId).ifPresent(t -> topicRepository.deleteById(id));
    }

    public List<QuizQuestion> getQuestionsDueTodayForOwner(Long ownerId) {
        return questionRepository.findDueForReviewByOwner(LocalDate.now(), ownerId);
    }

    public java.util.Map<String, Object> getAnalyticsForOwner(Long ownerId) {
        List<QuizQuestion> all = questionRepository.findAllByOwner(ownerId);

        long totalAnswered = all.stream().mapToLong(QuizQuestion::getTimesAnswered).sum();
        long totalCorrect = all.stream().mapToLong(QuizQuestion::getTimesCorrect).sum();
        double accuracyRate = totalAnswered == 0 ? 0.0 : (100.0 * totalCorrect / totalAnswered);

        java.util.Map<String, Long> byDifficulty = new java.util.HashMap<>();
        for (String level : List.of("EASY", "MEDIUM", "HARD")) {
            long count = all.stream()
                    .filter(q -> level.equalsIgnoreCase(q.getDifficulty()))
                    .count();
            byDifficulty.put(level, count);
        }

        long masteredCount = all.stream().filter(QuizQuestion::isMasteredFlag).count();

        return java.util.Map.of(
                "accuracyRate", Math.round(accuracyRate * 10.0) / 10.0,
                "totalAnswered", totalAnswered,
                "totalCorrect", totalCorrect,
                "byDifficulty", byDifficulty,
                "masteredCount", masteredCount,
                "totalQuestions", all.size()
        );
    }
}
