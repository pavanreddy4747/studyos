package com.studyos.app.controller;

import com.studyos.app.model.QuizQuestion;
import com.studyos.app.repository.QuizQuestionRepository;
import com.studyos.app.service.StudyTopicService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private QuizQuestionRepository questionRepository;

    @GetMapping("/today")
    public Map<String, Object> todayPlan() {
        List<QuizQuestion> dueToday = topicService.getQuestionsDueToday();

        long totalQuestions = questionRepository.count();
        long masteredCount = questionRepository.findAll().stream()
                .filter(QuizQuestion::isMasteredFlag).count();
        long totalTopics = topicService.getAllTopics().size();

        return Map.of(
                "dueToday", dueToday,
                "dueTodayCount", dueToday.size(),
                "totalQuestions", totalQuestions,
                "masteredCount", masteredCount,
                "totalTopics", totalTopics
        );
    }
}
