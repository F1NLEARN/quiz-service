package com.finlearn.quizservice.quizetl.domain.repository;

import com.finlearn.quizservice.quizetl.domain.entity.Quiz;

public interface QuizRepository {
    Quiz save(Quiz quiz);
}
