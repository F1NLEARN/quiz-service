package com.finlearn.quizservice.quizetl.infrastructure.repository;

import com.finlearn.quizservice.quizetl.domain.entity.CrawledSource;
import com.finlearn.quizservice.quizetl.domain.repository.CrawledSourceRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CrawledSourceJpaRepository extends JpaRepository<CrawledSource, UUID>, CrawledSourceRepository {
}
