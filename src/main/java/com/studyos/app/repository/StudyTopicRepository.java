package com.studyos.app.repository;

import com.studyos.app.model.StudyTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudyTopicRepository extends JpaRepository<StudyTopic, Long> {
    List<StudyTopic> findByOwnerId(Long ownerId);
}
