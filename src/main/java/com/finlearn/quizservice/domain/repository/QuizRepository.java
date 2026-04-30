package com.finlearn.quizservice.domain.repository;

import com.finlearn.quizservice.domain.entity.Quiz;

public interface QuizRepository {
    Quiz save(Quiz quiz);
}
