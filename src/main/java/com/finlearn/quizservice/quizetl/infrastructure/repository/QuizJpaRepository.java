package com.finlearn.quizservice.quizetl.infrastructure.repository;

import com.finlearn.quizservice.quizetl.domain.entity.Quiz;
import com.finlearn.quizservice.quizetl.domain.enums.MainTopic;
import com.finlearn.quizservice.quizetl.domain.repository.QuizRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizJpaRepository extends JpaRepository<Quiz, UUID>, QuizRepository {

    /**
     * 특정 카테고리(MainTopic)에서 랜덤으로 문제 ID를 조회한다.
     * 퀴즈 세션 생성 시 학습 퀴즈 출제에 사용된다.
     */
    @Query(value = "SELECT q.quizzes_id FROM quizzes q WHERE q.main_topic = :mainTopic ORDER BY RANDOM() LIMIT :limit",
            nativeQuery = true)
    List<UUID> findRandomIdsByMainTopic(@Param("mainTopic") String mainTopic, @Param("limit") int limit);

    /**
     * 전 카테고리에서 랜덤으로 문제 ID를 조회한다.
     * 퀴즈 세션 생성 시 포인트 퀴즈 출제에 사용된다.
     */
    @Query(value = "SELECT q.quizzes_id FROM quizzes q ORDER BY RANDOM() LIMIT :limit",
            nativeQuery = true)
    List<UUID> findRandomIds(@Param("limit") int limit);

    /**
     * 특정 카테고리의 전체 문제 ID를 조회한다.
     * Redis ID 풀 초기화 및 갱신에 사용된다.
     */
    @Query("SELECT q.id FROM Quiz q WHERE q.mainTopic = :mainTopic")
    List<UUID> findAllIdsByMainTopic(@Param("mainTopic") MainTopic mainTopic);

    /**
     * 전 카테고리의 전체 문제 ID를 조회한다.
     * Redis ID 풀 초기화 및 갱신에 사용된다.
     */
    @Query("SELECT q.id FROM Quiz q")
    List<UUID> findAllIds();
}
