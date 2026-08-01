package com.studyos.app.dto;

public class ChatTutorRequest {
    private Long topicId;
    private String question;

    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
}
