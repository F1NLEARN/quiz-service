package com.finlearn.quizservice.quizetl.application.service;

import com.finlearn.quizservice.quizetl.domain.entity.Quiz;
import com.finlearn.quizservice.quizetl.infrastructure.repository.QuizJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    /**
     * 여러 Quiz를 단일 IN 쿼리로 일괄 조회하여 Map으로 반환한다.
     * N+1 방지 목적으로 사용하며, 조회 결과는 개별 캐시(@Cacheable)와 별도로 관리된다.
     *
     * @param ids Quiz UUID 목록
     * @return UUID → Quiz 매핑
     */
    @Transactional(readOnly = true)
    public Map<UUID, Quiz> findAllByIds(List<UUID> ids) {
        return quizJpaRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(Quiz::getId, q -> q));
    }
}
