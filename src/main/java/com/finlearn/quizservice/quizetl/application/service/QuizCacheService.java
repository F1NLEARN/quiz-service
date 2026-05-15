package com.finlearn.quizservice.quizetl.application.service;

import com.finlearn.quizservice.quizetl.domain.entity.Quiz;
import com.finlearn.quizservice.quizetl.infrastructure.repository.QuizJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Quiz 조회 캐시 서비스.
 *
 * Cache-Aside 패턴으로 Quiz를 캐싱한다.
 * 첫 조회 시 DB에서 가져와 Redis에 적재하고, 이후 동일 ID 요청은 캐시에서 반환한다.
 * Quiz는 ETL로만 변경되므로 @CacheEvict 없이 TTL 기반 만료를 사용한다.
 *
 * 캐시 키: {@code quiz::{uuid}}, TTL: 12시간 (RedisConfig 기본값)
 */
@Service
@RequiredArgsConstructor
public class QuizCacheService {

    private final QuizJpaRepository quizJpaRepository;

    /**
     * Quiz를 캐시에서 조회하고, 미스 시 DB에서 조회 후 적재한다.
     *
     * @param id Quiz UUID
     * @return Quiz 엔티티, 존재하지 않으면 null
     */
    @Cacheable(value = "quiz", key = "#id")
    @Transactional(readOnly = true)
    public Quiz findById(UUID id) {
        return quizJpaRepository.findById(id).orElse(null);
    }
}
