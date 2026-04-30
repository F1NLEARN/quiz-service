package com.finlearn.quizservice.domain.repository;

import com.finlearn.quizservice.domain.entity.QuizTopic;
import com.finlearn.quizservice.domain.enums.TopicStatus;
import java.util.List;

public interface QuizTopicRepository {
    boolean existsBySubTopic(String subTopic);

    List<QuizTopic> findByStatus(TopicStatus status);

    QuizTopic save(QuizTopic topic);

    <S extends QuizTopic> List<S> saveAll(Iterable<S> topics);

    List<QuizTopic> findAll();
}
