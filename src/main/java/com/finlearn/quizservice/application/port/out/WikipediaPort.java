package com.finlearn.quizservice.application.port.out;

public interface WikipediaPort {
    String fetchArticleContent(String keyword);

    String generateArticleUrl(String keyword);
}