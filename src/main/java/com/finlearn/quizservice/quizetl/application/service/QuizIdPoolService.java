package com.finlearn.quizservice.quizetl.application.service;

import com.finlearn.quizservice.quizetl.domain.enums.MainTopic;
import com.finlearn.quizservice.quizetl.infrastructure.repository.QuizJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Quiz ID 풀 관리 서비스.
 *
 * <p>카테고리별 전체 Quiz ID를 Redis Set에 적재하고,
 * 세션 생성 시 {@code SRANDMEMBER}로 랜덤 추출하여 {@code ORDER BY RANDOM()} DB 쿼리를 대체한다.</p>
 *
 * <ul>
 *   <li>초기 로딩: 애플리케이션 기동 완료 시 {@link ApplicationReadyEvent}</li>
 *   <li>주기 갱신: 매일 새벽 3시 {@link #refresh()} — ETL 반영</li>
 *   <li>폴백: Redis 풀이 비어있으면 DB {@code ORDER BY RANDOM()} 사용</li>
 * </ul>
 *
 * Redis 키 패턴: {@code quiz:pool:{MAIN_TOPIC}}, {@code quiz:pool:ALL}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizIdPoolService {

    private static final String POOL_KEY_PREFIX = "quiz:pool:";
    private static final String ALL_KEY = POOL_KEY_PREFIX + "ALL";

    private final QuizJpaRepository quizJpaRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * 애플리케이션 기동 완료 후 전체 Quiz ID 풀을 Redis에 적재한다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("[QuizIdPool] 초기 ID 풀 로딩 시작");
        refresh();
    }

    /**
     * 매일 새벽 3시 ID 풀을 갱신한다. ETL로 추가/삭제된 문제를 반영한다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional(readOnly = true)
    public void refresh() {
        // 카테고리별 적재
        for (MainTopic topic : MainTopic.values()) {
            String key = POOL_KEY_PREFIX + topic.name();
            List<UUID> ids = quizJpaRepository.findAllIdsByMainTopic(topic);
            loadToRedis(key, ids);
        }

        // 전 카테고리 적재 (포인트 퀴즈용)
        List<UUID> allIds = quizJpaRepository.findAllIds();
        loadToRedis(ALL_KEY, allIds);

        log.info("[QuizIdPool] ID 풀 갱신 완료 - 전체 {}개", allIds.size());
    }

    /**
     * 특정 카테고리에서 랜덤 Quiz ID를 추출한다.
     * Redis 풀이 비어있으면 DB 폴백.
     *
     * @param mainTopic 카테고리
     * @param count     추출 개수
     * @return 랜덤 Quiz ID 목록
     */
    public List<UUID> getRandomIds(MainTopic mainTopic, int count) {
        String key = POOL_KEY_PREFIX + mainTopic.name();
        Set<String> members = redisTemplate.opsForSet().distinctRandomMembers(key, count);

        if (members == null || members.isEmpty()) {
            log.warn("[QuizIdPool] Redis 풀 없음 - DB 폴백 (category: {})", mainTopic);
            return quizJpaRepository.findRandomIdsByMainTopic(mainTopic.name(), count);
        }

        return members.stream().map(UUID::fromString).toList();
    }

    /**
     * 전 카테고리에서 랜덤 Quiz ID를 추출한다.
     * Redis 풀이 비어있으면 DB 폴백.
     *
     * @param count 추출 개수
     * @return 랜덤 Quiz ID 목록
     */
    public List<UUID> getRandomIds(int count) {
        Set<String> members = redisTemplate.opsForSet().distinctRandomMembers(ALL_KEY, count);

        if (members == null || members.isEmpty()) {
            log.warn("[QuizIdPool] Redis 풀 없음 - DB 폴백 (ALL)");
            return quizJpaRepository.findRandomIds(count);
        }

        return members.stream().map(UUID::fromString).toList();
    }

    /**
     * UUID 목록을 Redis Set에 적재한다. 기존 키를 삭제 후 재적재하여 최신 상태를 보장한다.
     */
    private void loadToRedis(String key, List<UUID> ids) {
        if (ids.isEmpty()) {
            log.warn("[QuizIdPool] 적재할 ID 없음 - key: {}", key);
            return;
        }

        String[] members = ids.stream().map(UUID::toString).toArray(String[]::new);
        redisTemplate.delete(key);
        redisTemplate.opsForSet().add(key, members);

        log.debug("[QuizIdPool] 적재 완료 - key: {}, count: {}", key, ids.size());
    }
}
