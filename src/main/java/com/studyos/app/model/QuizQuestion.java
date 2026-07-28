package com.studyos.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "topic_id", nullable = false)
    @JsonIgnore
    private StudyTopic topic;

    @Column(length = 1000)
    private String questionText;

    @Column(length = 2000)
    private String optionsRaw;

    private String correctAnswer;
    @Column(length = 1000)
    private String explanation;

    private String difficulty = "MEDIUM";

    private int correctStreak = 0;
    private int timesAnswered = 0;
    private int timesCorrect = 0;
    private LocalDate nextReviewDate;
    private boolean masteredFlag = false;

    public QuizQuestion() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public StudyTopic getTopic() { return topic; }
    public void setTopic(StudyTopic topic) { this.topic = topic; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getOptionsRaw() { return optionsRaw; }
    public void setOptionsRaw(String optionsRaw) { this.optionsRaw = optionsRaw; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getCorrectStreak() { return correctStreak; }
    public void setCorrectStreak(int correctStreak) { this.correctStreak = correctStreak; }

    public int getTimesAnswered() { return timesAnswered; }
    public void setTimesAnswered(int timesAnswered) { this.timesAnswered = timesAnswered; }

    public int getTimesCorrect() { return timesCorrect; }
    public void setTimesCorrect(int timesCorrect) { this.timesCorrect = timesCorrect; }

    public LocalDate getNextReviewDate() { return nextReviewDate; }
    public void setNextReviewDate(LocalDate nextReviewDate) { this.nextReviewDate = nextReviewDate; }

    public boolean isMasteredFlag() { return masteredFlag; }
    public void setMasteredFlag(boolean masteredFlag) { this.masteredFlag = masteredFlag; }
}