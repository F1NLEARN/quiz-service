package com.finlearn.quizservice.infrastructure.repository;

import com.finlearn.quizservice.domain.entity.CrawledSource;
import com.finlearn.quizservice.domain.repository.CrawledSourceRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CrawledSourceJpaRepository extends JpaRepository<CrawledSource, UUID>, CrawledSourceRepository {
}
