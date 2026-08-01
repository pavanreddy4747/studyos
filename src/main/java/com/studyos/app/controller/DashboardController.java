package com.studyos.app.controller;

import com.studyos.app.model.QuizQuestion;
import com.studyos.app.model.User;
import com.studyos.app.service.CurrentUserService;
import com.studyos.app.service.StudyTopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private StudyTopicService topicService;

    @Autowired
    private CurrentUserService currentUserService;

    @GetMapping("/today")
    public Map<String, Object> todayPlan(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        List<QuizQuestion> dueToday = topicService.getQuestionsDueTodayForOwner(user.getId());

        List<com.studyos.app.model.StudyTopic> topics = topicService.getTopicsForOwner(user.getId());
        long totalQuestions = topics.stream().mapToLong(t -> t.getQuestions().size()).sum();
        long masteredCount = topics.stream()
                .flatMap(t -> t.getQuestions().stream())
                .filter(QuizQuestion::isMasteredFlag)
                .count();

        return Map.of(
                "dueToday", dueToday,
                "dueTodayCount", dueToday.size(),
                "totalQuestions", totalQuestions,
                "masteredCount", masteredCount,
                "totalTopics", topics.size(),
                "currentStreak", user.getCurrentStreak()
        );
    }

    @GetMapping("/analytics")
    public Map<String, Object> analytics(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        return topicService.getAnalyticsForOwner(user.getId());
    }
}
