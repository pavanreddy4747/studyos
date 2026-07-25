package com.studyos.app.dto;

public class SubmitAnswerResponse {
    private boolean correct;
    private String correctAnswer;
    private String explanation;

    public SubmitAnswerResponse(boolean correct, String correctAnswer, String explanation) {
        this.correct = correct;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
    }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
