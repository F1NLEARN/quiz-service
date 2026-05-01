package com.finlearn.quizservice.infrastructure.repository;

import com.finlearn.quizservice.domain.entity.QuizTopic;
import com.finlearn.quizservice.domain.repository.QuizTopicRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizTopicJpaRepository extends JpaRepository<QuizTopic, UUID>, QuizTopicRepository {
}
