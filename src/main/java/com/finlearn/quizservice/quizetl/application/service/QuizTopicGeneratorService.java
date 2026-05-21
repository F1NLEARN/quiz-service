package com.finlearn.quizservice.quizetl.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.common.exception.InternalServerException;
import com.finlearn.quizservice.quizetl.domain.entity.QuizTopic;
import com.finlearn.quizservice.quizetl.domain.enums.MainTopic;
import com.finlearn.quizservice.quizetl.domain.repository.QuizTopicRepository;
import com.finlearn.quizservice.quizetl.infrastructure.kafka.event.QuizTopicCreatedEvent;
import com.finlearn.quizservice.quizsession.infrastructure.outbox.OutboxEvent;
import com.finlearn.quizservice.quizsession.infrastructure.outbox.OutboxEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class QuizTopicGeneratorService {

    private final ChatClient chatClient;
    private final QuizTopicRepository quizTopicRepository;
    private final TransactionTemplate transactionTemplate;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.quiz.topic-created:finlearn-quiz-topic-created}")
    private String topicTopicCreated;

    public QuizTopicGeneratorService(ChatClient.Builder chatClientBuilder, QuizTopicRepository quizTopicRepository,
            ChatMemory chatMemory, TransactionTemplate transactionTemplate, OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.quizTopicRepository = quizTopicRepository;
        this.transactionTemplate = transactionTemplate;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public record KeywordResponse(List<String> keywords) {
    }

    private static final int DEFAULT_TOPIC_COUNT = 10;

    public void generateAllTopicsKeywords(UUID userId, String email, String role) {
        generateAllTopicsKeywords(DEFAULT_TOPIC_COUNT, userId, email, role);
    }

    public void generateAllTopicsKeywords(int count, UUID userId, String email, String role) {
        for (MainTopic topic : MainTopic.values()) {
            log.info("'{}' 대주제에 대한 소주제 생성 배치 시작", topic.getDescription());
            generateAndSaveTopics(topic, count);

            // 대주제 간 Rate Limit 방어를 위해 5초 대기
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                log.warn("대주제 생성 루프가 인터럽트되었습니다.");
                Thread.currentThread().interrupt();
                break;
            }
        }

        transactionTemplate.executeWithoutResult(status -> {
            QuizTopicCreatedEvent event = new QuizTopicCreatedEvent(userId, email, role, LocalDateTime.now());

            try {
                String json = objectMapper.writeValueAsString(event);
                outboxEventRepository.save(
                        OutboxEvent.create(topicTopicCreated, userId != null ? userId.toString() : "SYSTEM", json));
                log.info("topicCreated 이벤트 outbox에 저장 완료");
            } catch (JsonProcessingException e) {
                throw new InternalServerException("이벤트 직렬화 실패: " + e.getMessage());
            }
        });
    }

    public List<String> generateAndSaveTopics(MainTopic mainTopic, int count) {
        log.info("{} 대주제에 대한 {}개의 소주제 생성", mainTopic.getDescription(), count);

        // 프롬프트 무작위성(Angle) 부여
        String[] angles = { "기초적이고 대중적인 필수 개념", "최신 금융 트렌드 및 이슈가 반영된 개념", "투자 전략 및 실무 분석과 관련된 개념",
                "투자 리스크, 함정, 주의사항과 관련된 개념", "실생활에서 바로 써먹을 수 있는 금융 상식", "뉴스에서 자주 나오는 경제 용어", "재테크 초보자가 흔히 하는 실수와 오해" };
        String selectedAngle = angles[new Random().nextInt(angles.length)];
        log.info("선택된 소주제 생성 방향: {}", selectedAngle);

        String prompt = String.format(
                "당신은 투자 초보자를 위한 최고의 금융 투자 교육 전문가입니다. "
                        + "'%s' 대주제와 관련하여, 초보 투자자가 주식, ETF, 선물 거래를 시작하기 전에 반드시 이해해야 하는 %s 위주로 핵심 투자/경제 용어(명사형)를 %d개 추천해주세요.\n\n"
                        + "[주의사항]\n"
                        + "1. 단순 과학기술, 하드웨어 제품, 공학/제조업 산업 분야의 용어(예: HBM, 자율주행, 배터리 등)는 절대 포함하지 마세요.\n"
                        + "2. **은행 가계 대출, 주택담보대출, 담보, 보증, 예금자보호 등 시중은행의 대출 및 예대 업무 중심의 금융 상식 용어는 투자 교육이라는 우리 플랫폼의 목적과 무관하므로 절대 제외하세요**.\n"
                        + "3. 주식/ETF/선물 거래 제도, 투자 기초 경제원리(인플레이션, 복리, 유동성 등), 자산 배분 전략, 기업가치 분석 지표(PER, PBR 등), 거래 주문 방식 등 **'순수 금융 투자 및 자본시장 거래'와 직접 관련된 기초 용어**만 생성해 주세요.\n"
                        + "4. 이 용어들은 한국어 위키피디아의 문서 제목으로 정확히 검색될 만큼 보편적이어야 합니다. "
                        + "예를 들어 '분산 투자', '주가수익률', '상장지수펀드', '배당락', '예수금', '지정가 주문', '레버리지' 등입니다. 부연 설명 없이 단어들만 배열로 주세요.",
                mainTopic.getDescription(), selectedAngle, count);

        // AI 호출
        KeywordResponse response = callChatWithRetry(mainTopic, prompt);

        List<String> keywords = response.keywords();
        log.info("생성 완료된 키워드: {}", keywords);

        // QuizTopic 중복 검사 및 저장
        transactionTemplate.executeWithoutResult(status -> {
            List<QuizTopic> newTopics = keywords.stream()
                    .filter(keyword -> !quizTopicRepository.existsBySubTopic(keyword))
                    .map(keyword -> QuizTopic.builder().mainTopic(mainTopic).subTopic(keyword).build()).toList();

            if (!newTopics.isEmpty()) {
                quizTopicRepository.saveAll(newTopics);
                log.info("{}개의 새로운 키워드가 DB에 저장되었습니다.", newTopics.size());
            } else {
                log.info("생성된 모든 키워드가 이미 DB에 존재하여 저장하지 않았습니다.");
            }
        });

        return keywords;
    }

    private KeywordResponse callChatWithRetry(MainTopic mainTopic, String prompt) {
        int maxAttempts = 5;
        int delayMs = 60000; // 60 seconds
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return chatClient.prompt()
                        .advisors(a -> a.param("chat_memory_conversation_id", mainTopic.name()))
                        .user(prompt)
                        .call()
                        .entity(KeywordResponse.class);
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    throw e;
                }
                if (isRateLimitException(e)) {
                    log.warn("Gemini API Rate Limit (429) 감지됨. {}초 대기 후 재시도합니다... (시도 {}/{})", delayMs / 1000, attempt, maxAttempts);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("API 재시도 대기 중 인터럽트 발생", ie);
                    }
                } else {
                    log.warn("Gemini API 호출 중 오류 발생. 5초 후 재시도합니다... (시도 {}/{}) - 에러: {}", attempt, maxAttempts, e.getMessage());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("API 재시도 대기 중 인터럽트 발생", ie);
                    }
                }
            }
        }
        throw new RuntimeException("모든 API 재시도 횟수 초과");
    }

    private boolean isRateLimitException(Throwable e) {
        if (e == null) {
            return false;
        }
        String msg = e.getMessage();
        if (msg != null && (msg.contains("429") || msg.contains("quota") || msg.contains("Limit") || msg.contains("Rate"))) {
            return true;
        }
        return isRateLimitException(e.getCause());
    }
}
