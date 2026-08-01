package com.studyos.app.dto;

import java.util.List;

public class GeneratedContent {

    private String summary;
    private List<GeneratedQuestion> questions;
    private List<GeneratedFlashcard> flashcards;

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<GeneratedQuestion> getQuestions() { return questions; }
    public void setQuestions(List<GeneratedQuestion> questions) { this.questions = questions; }

    public List<GeneratedFlashcard> getFlashcards() { return flashcards; }
    public void setFlashcards(List<GeneratedFlashcard> flashcards) { this.flashcards = flashcards; }

    public static class GeneratedQuestion {
        private String question;
        private List<String> options;
        private String correctAnswer;
        private String explanation;
        private String difficulty;

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
        public String getCorrectAnswer() { return correctAnswer; }
        public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    }

    public static class GeneratedFlashcard {
        private String front;
        private String back;

        public String getFront() { return front; }
        public void setFront(String front) { this.front = front; }
        public String getBack() { return back; }
        public void setBack(String back) { this.back = back; }
    }
}
