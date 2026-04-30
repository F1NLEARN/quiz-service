package com.finlearn.quizservice.domain.repository;

import com.finlearn.quizservice.domain.entity.CrawledSource;

public interface CrawledSourceRepository {
    boolean existsBySourceUrl(String sourceUrl);
    CrawledSource save(CrawledSource source);
}
