package com.finlearn.quizservice.quizetl.application.port.out;

public interface WikipediaPort {
    String fetchArticleContent(String keyword);

    String generateArticleUrl(String keyword);
}