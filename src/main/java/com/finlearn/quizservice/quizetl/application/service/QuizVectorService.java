package com.finlearn.quizservice.quizetl.application.service;

import com.finlearn.quizservice.quizetl.domain.entity.CrawledSource;
import com.finlearn.quizservice.quizetl.domain.entity.QuizTopic;
import com.finlearn.quizservice.quizetl.domain.enums.TopicStatus;
import com.finlearn.quizservice.quizetl.domain.repository.CrawledSourceRepository;
import com.finlearn.quizservice.quizetl.domain.repository.QuizTopicRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizVectorService {

    private final QuizTopicRepository quizTopicRepository;
    private final CrawledSourceRepository crawledSourceRepository;
    private final VectorStore vectorStore;

    public void vectorizeCrawledTopics() {
        log.info("수집 완료된 모든 소주제들에 대한 벡터화를 시작합니다.");
        List<QuizTopic> crawledTopics = quizTopicRepository.findByStatus(TopicStatus.CRAWLED);
        log.info("벡터화 대기 중인 소주제 {}개", crawledTopics.size());

        for (QuizTopic topic : crawledTopics) {
            try {
                processTopic(topic);
            } catch (Exception e) {
                log.error("'{}' 소주제 처리 중 오류 발생: {}", topic.getSubTopic(), e.getMessage());
            }
        }
    }

    @Transactional
    public void processTopic(QuizTopic topic) {
        String keyword = topic.getSubTopic();
        crawledSourceRepository.findByKeyword(keyword).ifPresentOrElse(source -> {
            vectorizeSource(topic, source);
            topic.updateStatus(TopicStatus.EMBEDDED);
            quizTopicRepository.save(topic);
            log.info("'{}' 소주제 벡터화 및 상태 업데이트 완료", keyword);
        }, () -> log.warn("'{}' 소주제의 크롤링 소스를 찾을 수 없습니다.", keyword));
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

        for (int i = 0; i < splitDocs.size(); i++) {
            Document doc = splitDocs.get(i);
            // RDB랑 벡터DB는 같은 커넥션 풀을 쓰지 않는다고 함. 멱등성 보장을 위해 "topicId-CONTENT-index" 형태의 고유 ID 부여
            String deterministicIdStr = String.format("%s-CONTENT-%d", topic.getId(), i);
            String docId = UUID.nameUUIDFromBytes(deterministicIdStr.getBytes(StandardCharsets.UTF_8)).toString();
            Document idempotentDoc = new Document(docId, doc.getText(), doc.getMetadata());

            vectorStore.accept(List.of(idempotentDoc));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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
        java.util.List<String> result = new java.util.ArrayList<>();

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

        java.util.Collections.reverse(result);
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
