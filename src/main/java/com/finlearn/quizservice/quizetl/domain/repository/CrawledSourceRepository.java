package com.finlearn.quizservice.quizetl.domain.repository;

import com.finlearn.quizservice.quizetl.domain.entity.CrawledSource;
import java.util.List;
import java.util.Optional;

public interface CrawledSourceRepository {
    boolean existsBySourceUrl(String sourceUrl);

    CrawledSource save(CrawledSource source);

    Optional<CrawledSource> findByKeyword(String keyword);

    List<CrawledSource> findAll();
}
