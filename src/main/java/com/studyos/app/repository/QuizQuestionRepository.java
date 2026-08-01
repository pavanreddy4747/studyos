package com.studyos.app.repository;

import com.studyos.app.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByTopicId(Long topicId);

    @Query("SELECT q FROM QuizQuestion q WHERE q.nextReviewDate <= :today AND q.masteredFlag = false AND q.topic.owner.id = :ownerId")
    List<QuizQuestion> findDueForReviewByOwner(@Param("today") LocalDate today, @Param("ownerId") Long ownerId);

    @Query("SELECT q FROM QuizQuestion q WHERE q.masteredFlag = false AND q.topic.owner.id = :ownerId")
    List<QuizQuestion> findAllUnmasteredByOwner(@Param("ownerId") Long ownerId);

    @Query("SELECT q FROM QuizQuestion q WHERE q.topic.owner.id = :ownerId")
    List<QuizQuestion> findAllByOwner(@Param("ownerId") Long ownerId);
}
