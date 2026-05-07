package com.finlearn.quizservice.quizetl.infrastructure.repository;

import com.finlearn.quizservice.quizetl.domain.entity.QuizTopic;
import com.finlearn.quizservice.quizetl.domain.repository.QuizTopicRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizTopicJpaRepository extends JpaRepository<QuizTopic, UUID>, QuizTopicRepository {
}
