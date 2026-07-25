package com.studyos.app.repository;

import com.studyos.app.model.StudyTopic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyTopicRepository extends JpaRepository<StudyTopic, Long> {
}
