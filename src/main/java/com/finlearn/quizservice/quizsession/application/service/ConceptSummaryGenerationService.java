package com.finlearn.quizservice.quizsession.application.service;

import com.finlearn.quizservice.quizsession.domain.QuizSession;
import com.finlearn.quizservice.quizsession.domain.QuizSessionQuiz;
import com.finlearn.quizservice.quizsession.domain.SessionConceptSummary;
import com.finlearn.quizservice.quizsession.domain.repository.SessionConceptSummaryRepository;
import com.finlearn.quizservice.quizetl.application.service.QuizCacheService;
import com.finlearn.quizservice.quizetl.domain.entity.Quiz;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 세션 개념 정리 비동기 생성 서비스.
 *
 * 세션 종료 시 is_concept_included=true인 문제의 answer_explanation을 수집하여
 * ChatClient(Gemini)로 개념 정리를 생성하고 DB에 저장한다.
 *
 * - @Async: 세션 종료 응답에 영향을 주지 않는다.
 * - 생성 실패 시 예외를 삼키고 로그만 기록한다 (세션 종료와 독립).
 * - RAG 미사용: answer_explanation이 이미 정제된 해설이므로 벡터 검색 불필요.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptSummaryGenerationService {

    private final ChatClient chatClient;
    private final QuizCacheService quizCacheService;
    private final SessionConceptSummaryRepository sessionConceptSummaryRepository;

    @Async
    public void generateAsync(QuizSession session) {
        try {
            // 개념 정리 대상 문제 필터링
            List<QuizSessionQuiz> targets = session.getQuizzes().stream()
                    .filter(QuizSessionQuiz::isConceptIncluded)
                    .collect(Collectors.toList());

            if (targets.isEmpty()) {
                log.info("[ConceptSummary] 개념 정리 대상 문제 없음 - sessionId: {}", session.getId().value());
                return;
            }

            // 대상 문제 ID 목록 추출
            List<UUID> quizIds = targets.stream()
                    .map(q -> q.getQuizId().value())
                    .toList();

            // 단일 IN 쿼리로 일괄 조회 → Map 인덱싱 (N+1 방지)
            Map<UUID, Quiz> quizMap = quizCacheService.findAllByIds(quizIds);

            // answerExplanation 수집
            String explanations = targets.stream()
                    .map(q -> {
                        Quiz quiz = quizMap.get(q.getQuizId().value());
                        return quiz != null ? quiz.getAnswerExplanation() : "";
                    })
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining("\n\n---\n\n"));

            if (explanations.isBlank()) {
                log.warn("[ConceptSummary] 해설 데이터 없음 - sessionId: {}", session.getId().value());
                return;
            }

            // 프롬프트 구성 및 LLM 호출
            String prompt = buildPrompt(explanations, targets.size());
            String summaryContent = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 저장
            SessionConceptSummary summary = SessionConceptSummary.create(
                    session.getId(),
                    session.getUserId(),
                    summaryContent
            );
            sessionConceptSummaryRepository.save(summary);

            log.info("[ConceptSummary] 생성 완료 - sessionId: {}, 문제수: {}",
                    session.getId().value(), targets.size());

        } catch (Exception e) {
            log.error("[ConceptSummary] 생성 실패 - sessionId: {}", session.getId().value(), e);
        }
    }

    private String buildPrompt(String explanations, int count) {
        return String.format("""
                아래는 금융 퀴즈 %d문제의 해설입니다.
                각 해설의 핵심 개념을 정리하여 사용자가 복습하기 좋은 형태로 요약해주세요.

                요구사항:
                - 한국어로 작성
                - 문제별로 핵심 개념을 명확히 구분
                - 금융 용어는 쉽게 풀어서 설명
                - 외울 만한 핵심 포인트를 강조

                [해설 목록]
                %s
                """, count, explanations);
    }
}
