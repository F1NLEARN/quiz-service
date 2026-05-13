package com.finlearn.quizservice.quizsession.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * VectorStore(pgvector)에서 관련 문서를 검색하여 RAG 컨텍스트를 반환한다.
 *
 * QuizVectorService가 저장한 type=CONTENT 문서를 subTopic 기준으로 검색한다.
 * 검색 실패 시 빈 문자열을 반환하여 ChatbotService의 처리를 중단시키지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagContextRetriever {

    private final VectorStore vectorStore;

    private static final int TOP_K = 3;

    /**
     * subTopic에 해당하는 Wikipedia 문서 청크를 검색하여 컨텍스트 텍스트를 반환한다.
     *
     * @param subTopic 검색할 키워드 (Quiz 엔티티의 subTopic 필드)
     * @return 검색된 문서 내용을 합친 컨텍스트. 없으면 빈 문자열
     */
    public String retrieve(String subTopic) {
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(subTopic)
                    .topK(TOP_K)
                    .filterExpression("type == 'CONTENT' && subTopic == '" + subTopic + "'")
                    .build();

            String context = vectorStore.similaritySearch(request).stream()
                    .map(doc -> doc.getText())
                    .collect(Collectors.joining("\n\n"));

            if (context.isBlank()) {
                log.warn("[RAG] '{}' 에 대한 벡터 컨텍스트 없음 — 일반 지식으로 응답", subTopic);
            }
            return context;
        } catch (Exception e) {
            log.error("[RAG] 컨텍스트 검색 실패 - subTopic: {}", subTopic, e);
            return "";
        }
    }
}
