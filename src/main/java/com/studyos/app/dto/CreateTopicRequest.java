package com.studyos.app.dto;

public class CreateTopicRequest {
    private String title;
    private String sourceText;
    private Integer questionCount;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSourceText() { return sourceText; }
    public void setSourceText(String sourceText) { this.sourceText = sourceText; }
    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
}
