package com.finlearn.quizservice.infrastructure.repository;

import com.finlearn.quizservice.domain.entity.QuizGenerationLog;
import com.finlearn.quizservice.domain.repository.QuizGenerationLogRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizGenerationLogJpaRepository
        extends JpaRepository<QuizGenerationLog, UUID>, QuizGenerationLogRepository {
}
