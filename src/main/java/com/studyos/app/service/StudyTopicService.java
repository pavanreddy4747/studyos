package com.studyos.app.service;

import com.studyos.app.dto.GeneratedContent;
import com.studyos.app.model.Flashcard;
import com.studyos.app.model.QuizQuestion;
import com.studyos.app.model.StudyTopic;
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

    /**
     * Creates a new study topic: calls Gemini to generate a summary, quiz, and
     * flashcards from the raw material, then persists everything.
     */
    public StudyTopic createTopicFromMaterial(String title, String sourceText) throws Exception {
        StudyTopic topic = new StudyTopic(title, sourceText);
        topic = topicRepository.save(topic);

        GeneratedContent generated = geminiService.generateStudyMaterial(sourceText);

        topic.setSummary(generated.getSummary());

        for (GeneratedContent.GeneratedQuestion gq : generated.getQuestions()) {
            QuizQuestion q = new QuizQuestion();
            q.setTopic(topic);
            q.setQuestionText(gq.getQuestion());
            q.setOptionsRaw(String.join("||", gq.getOptions()));
            q.setCorrectAnswer(gq.getCorrectAnswer());
            q.setExplanation(gq.getExplanation());
            q.setNextReviewDate(LocalDate.now()); // due immediately the first time
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

    public List<StudyTopic> getAllTopics() {
        return topicRepository.findAll();
    }

    public Optional<StudyTopic> getTopicById(Long id) {
        return topicRepository.findById(id);
    }

    public void deleteTopic(Long id) {
        topicRepository.deleteById(id);
    }

    public List<QuizQuestion> getQuestionsDueToday() {
        return questionRepository.findDueForReview(LocalDate.now());
    }

    public List<QuizQuestion> getAllUnmasteredQuestions() {
        return questionRepository.findAllUnmastered();
    }
}
