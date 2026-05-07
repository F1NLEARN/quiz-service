package com.finlearn.quizservice.quizetl.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizDeduplicationService {

    private final VectorStore vectorStore;

    // 코사인 유사도 기준치
    private static final double SIMILARITY_THRESHOLD = 0.80;

    // 새로 생성된 퀴즈가 기존에 생성되었던 퀴즈들과 유사한지 검사
    public boolean isTooSimilarToExistingQuizzes(String newQuizQuestion) {
        log.info("새로 생성된 퀴즈의 중복 여부를 검사합니다: '{}'", newQuizQuestion);

        SearchRequest searchRequest = SearchRequest.builder().query(newQuizQuestion) // 신규 질문을 벡터화하여 검색
                .topK(1) // 가장 유사한 1개만 조회
                .similarityThreshold(SIMILARITY_THRESHOLD) // 0.80 이상의 유사도만 필터링
                .filterExpression("type == 'QUIZ'") // 퀴즈 데이터끼리만 비교
                .build();

        List<Document> similarDocuments = vectorStore.similaritySearch(searchRequest);

        if (!similarDocuments.isEmpty()) {
            // 유사도 = 1.0 - distance
            double distance = similarDocuments.get(0).getMetadata().getOrDefault("distance", 0.0) instanceof Double d
                    ? d
                    : 0.0;
            double similarityScore = 1.0 - distance;

            log.warn("유사도: {} (임계치: {} 이상). 생성된 문제를 폐기합니다.", String.format("%.4f", similarityScore),
                    SIMILARITY_THRESHOLD);
            return true;
        }

        log.info("중복 검사 통과: {}", newQuizQuestion);
        return false;
    }
}
