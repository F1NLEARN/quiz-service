package com.finlearn.quizservice.quizetl.application.service;

import com.finlearn.quizservice.quizetl.domain.entity.CrawledSource;
import com.finlearn.quizservice.quizetl.domain.entity.Quiz;
import com.finlearn.quizservice.quizetl.domain.entity.QuizChoice;
import com.finlearn.quizservice.quizetl.domain.entity.QuizGenerationLog;
import com.finlearn.quizservice.quizetl.domain.enums.MainTopic;
import com.finlearn.quizservice.quizetl.domain.repository.CrawledSourceRepository;
import com.finlearn.quizservice.quizetl.domain.repository.QuizGenerationLogRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuizGenerationLogService {

    private final QuizGenerationLogRepository quizGenerationLogRepository;
    private final CrawledSourceRepository crawledSourceRepository;

    @Transactional
    public void saveSuccessLog(Quiz quiz) {
        quizGenerationLogRepository.save(QuizGenerationLog.success(quiz));
    }

    // 퀴즈 생성 실패 하더라도 로그 남기기 위해 새 트랜잭션으로 실행
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailureLog(MainTopic mainTopic, String keyword, QuizGeneratorService.QuizAiResponse response,
            String errorMessage) {
        CrawledSource source = crawledSourceRepository.findByKeyword(keyword).orElse(null);

        List<QuizChoice> choices = null;
        String title = null;
        String question = null;
        String explanation = null;

        if (response != null) {
            choices = response.choices().stream()
                    .map(c -> QuizChoice.builder().no(c.no()).content(c.content()).correct(c.correct()).build())
                    .collect(Collectors.toList());
            title = response.title();
            question = response.question();
            explanation = response.answerExplanation();
        }

        quizGenerationLogRepository.save(QuizGenerationLog.failed(source, mainTopic, keyword, title, question,
                explanation, choices, errorMessage));
    }
}
