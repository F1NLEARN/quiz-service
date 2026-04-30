package com.finlearn.quizservice.domain.repository;

import com.finlearn.quizservice.domain.entity.QuizGenerationLog;

public interface QuizGenerationLogRepository {
    QuizGenerationLog save(QuizGenerationLog log);
}
