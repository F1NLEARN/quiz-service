package com.finlearn.quizservice.quizetl.domain.repository;

import com.finlearn.quizservice.quizetl.domain.entity.QuizGenerationLog;

public interface QuizGenerationLogRepository {
    QuizGenerationLog save(QuizGenerationLog log);
}
