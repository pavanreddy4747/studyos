package com.studyos.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "flashcards")
public class Flashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "topic_id", nullable = false)
    @JsonIgnore
    private StudyTopic topic;

    @Column(length = 1000)
    private String front;

    @Column(length = 1000)
    private String back;

    public Flashcard() {}

    public Flashcard(StudyTopic topic, String front, String back) {
        this.topic = topic;
        this.front = front;
        this.back = back;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public StudyTopic getTopic() { return topic; }
    public void setTopic(StudyTopic topic) { this.topic = topic; }

    public String getFront() { return front; }
    public void setFront(String front) { this.front = front; }

    public String getBack() { return back; }
    public void setBack(String back) { this.back = back; }
}