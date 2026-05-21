package com.finlearn.quizservice.quizetl.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.common.exception.InternalServerException;
import com.finlearn.quizservice.quizetl.domain.entity.CrawledSource;
import com.finlearn.quizservice.quizetl.domain.entity.QuizTopic;
import com.finlearn.quizservice.quizetl.domain.enums.TopicStatus;
import com.finlearn.quizservice.quizetl.domain.repository.CrawledSourceRepository;
import com.finlearn.quizservice.quizetl.domain.repository.QuizTopicRepository;
import com.finlearn.quizservice.quizetl.infrastructure.kafka.event.QuizEmbeddedEvent;
import com.finlearn.quizservice.quizsession.infrastructure.outbox.OutboxEvent;
import com.finlearn.quizservice.quizsession.infrastructure.outbox.OutboxEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizVectorService {

    private final QuizTopicRepository quizTopicRepository;
    private final CrawledSourceRepository crawledSourceRepository;
    private final VectorStore vectorStore;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Value("${kafka.topics.quiz.embedded:finlearn-quiz-embedded}")
    private String topicEmbedded;

    public void vectorizeCrawledTopics(UUID userId, String email, String role) {
        log.info("수집 완료된 모든 소주제들에 대한 벡터화를 시작합니다.");
        List<QuizTopic> crawledTopics = quizTopicRepository.findByStatus(TopicStatus.CRAWLED);
        log.info("벡터화 대기 중인 소주제 {}개", crawledTopics.size());

        int successCount = 0;

        for (QuizTopic topic : crawledTopics) {
            try {
                processTopic(topic);
                successCount++;
            } catch (Exception e) {
                log.error("'{}' 소주제 처리 중 오류 발생 (해당 소주제를 FAILED로 마크하고 진행합니다): {}", topic.getSubTopic(), e.getMessage());
                
                // 트랜잭션 단위로 이 소주제의 상태를 FAILED로 격리 저장
                transactionTemplate.executeWithoutResult(status -> {
                    topic.updateStatus(TopicStatus.FAILED);
                    quizTopicRepository.save(topic);
                });
            }
        }

        if (successCount > 0) {
            log.info("벡터화 배치 끝. 성공한 토픽이 있으므로 embedded 이벤트 outbox에 저장 시작");

            // 모든 벡터화가 완료된 후 Outbox에 이벤트 저장
            transactionTemplate.executeWithoutResult(status -> {
                QuizEmbeddedEvent event = new QuizEmbeddedEvent(userId, email, role, LocalDateTime.now());

                try {
                    String json = objectMapper.writeValueAsString(event);
                    outboxEventRepository.save(
                            OutboxEvent.create(topicEmbedded, userId != null ? userId.toString() : "SYSTEM", json));
                    log.info("embedded 이벤트 outbox에 저장 완료");
                } catch (JsonProcessingException e) {
                    throw new InternalServerException("이벤트 직렬화 실패: " + e.getMessage());
                }
            });
        } else {
            log.info("벡터화 배치 끝. 성공한 토픽이 없어 이벤트를 발행하지 않습니다.");
        }
    }

    public void processTopic(QuizTopic topic) {
        transactionTemplate.executeWithoutResult(status -> {
            String keyword = topic.getSubTopic();
            crawledSourceRepository.findByKeyword(keyword).ifPresentOrElse(source -> {
                vectorizeSource(topic, source);
                topic.updateStatus(TopicStatus.EMBEDDED);
                quizTopicRepository.save(topic);
                log.info("'{}' 소주제 벡터화 및 상태 업데이트 완료", keyword);
            }, () -> {
                log.warn("'{}' 소주제의 크롤링 소스를 찾을 수 없습니다.", keyword);
                topic.updateStatus(TopicStatus.FAILED);
                quizTopicRepository.save(topic);
            });
        });
    }

    private void vectorizeSource(QuizTopic topic, CrawledSource source) {
        String content = source.getContent();
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        // 데이터 정제 (수식 및 노이즈 제거)
        String cleanedContent = cleanContent(content);

        // 정제 후 내용이 너무 짧으면 무시
        if (cleanedContent.length() < 20) {
            log.warn("'{}' 토픽의 정제된 내용이 너무 짧아 벡터화를 건너뜁니다.", topic.getSubTopic());
            return;
        }

        // 청킹
        TokenTextSplitter splitter = new TokenTextSplitter(400, 100, 5, 10000, true);

        List<Document> splitDocs = splitter.apply(List.of(new Document(cleanedContent,
                Map.of("type", "CONTENT", "sourceId", source.getId().toString(), "topicId", topic.getId().toString(),
                        "keyword", source.getKeyword(), "mainTopic", topic.getMainTopic().name(), "subTopic",
                        topic.getSubTopic()))));

        log.info("'{}' 소주제를 {}개의 청크로 분리하여 저장 시작", topic.getSubTopic(), splitDocs.size());

        List<Document> idempotentDocs = new ArrayList<>();
        for (int i = 0; i < splitDocs.size(); i++) {
            Document doc = splitDocs.get(i);
            // RDB랑 벡터DB는 같은 커넥션 풀을 쓰지 않는다고 함. 멱등성 보장을 위해 "topicId-CONTENT-index" 형태의 고유 ID 부여
            String deterministicIdStr = String.format("%s-CONTENT-%d", topic.getId(), i);
            String docId = UUID.nameUUIDFromBytes(deterministicIdStr.getBytes(StandardCharsets.UTF_8)).toString();
            idempotentDocs.add(new Document(docId, doc.getText(), doc.getMetadata()));
        }

        // 한 번에 모든 청크를 벡터 DB에 저장 (API 호출 1회로 압축)
        vectorStore.accept(idempotentDocs);

        try {
            // 구글 제미나이 무료 티어 제한(15 RPM)을 절대 넘지 않도록 5초 대기 (1분에 최대 12회 호출)
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String cleanContent(String content) {
        if (content == null)
            return "";

        // 1. 의미 없는 빈 섹션(내용 없는 헤더) 제거
        String filteredContent = removeEmptySections(content);

        // 2. HTML 태그 제거
        String cleaned = filteredContent.replaceAll("<[^>]*>", " ");

        // 3. 위키 기호(==)를 공백으로 변환 (구분자 역할)
        cleaned = cleaned.replaceAll("={2,}", " ");

        // 4. LaTeX 명령어 부분만 제거 (\text, \mathbf 등)
        // {내용} 및 수식 기호(_, ^)는 LLM의 문맥 이해를 위해 보존
        cleaned = cleaned.replaceAll("\\\\[a-zA-Z]+", " ");

        // 5. 연속된 가로 공백(스페이스, 탭) 정리 (줄바꿈 제외)
        cleaned = cleaned.replaceAll("[ \\t]+", " ");

        // 6. 전략적 줄바꿈 압축
        cleaned = cleaned.replaceAll("\\n{3,}", "___DOUBLE_NL___");
        cleaned = cleaned.replaceAll("\\n{2}", "\n");
        cleaned = cleaned.replaceAll("___DOUBLE_NL___", "\n\n");

        return cleaned.trim();
    }

    private String removeEmptySections(String content) {
        String[] lines = content.split("\n");
        List<String> result = new ArrayList<>();

        // 현재 유효하다고 판단된 가장 상위(숫자가 작은) 헤더 레벨
        int lastValidHeaderLevel = Integer.MAX_VALUE;
        boolean hasContentBelow = false;

        // 뒤에서부터 읽으면서 계층 구조를 고려하여 본문 없는 헤더 제거
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.isEmpty())
                continue;

            int currentLevel = getHeaderLevel(line);

            if (currentLevel > 0) { // 헤더인 경우
                // 1. 아래에 본문이 있고,
                // 2. 현재 헤더가 아래에서 발견된 가장 상위 헤더보다 더 상위 레벨(숫자가 작음)일 때만 유지
                if (hasContentBelow && currentLevel < lastValidHeaderLevel) {
                    result.add(lines[i]);
                    lastValidHeaderLevel = currentLevel;
                }
            } else { // 본문 텍스트 발견
                result.add(lines[i]);
                hasContentBelow = true;
                lastValidHeaderLevel = Integer.MAX_VALUE; // 본문이 나왔으므로 헤더 레벨 기준 초기화
            }
        }

        Collections.reverse(result);
        return String.join("\n", result);
    }

    private int getHeaderLevel(String line) {
        if (!line.startsWith("=="))
            return 0;
        int count = 0;
        while (count < line.length() && line.charAt(count) == '=') {
            count++;
        }
        return count;
    }
}
