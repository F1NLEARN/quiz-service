package com.finlearn.quizservice.infrastructure.repository;

import com.finlearn.quizservice.domain.entity.Quiz;
import com.finlearn.quizservice.domain.repository.QuizRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizJpaRepository extends JpaRepository<Quiz, UUID>, QuizRepository {
}
