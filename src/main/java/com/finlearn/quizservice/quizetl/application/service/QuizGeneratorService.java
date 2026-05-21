package com.finlearn.quizservice.quizetl.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.common.exception.InternalServerException;
import com.finlearn.quizservice.quizetl.domain.entity.CrawledSource;
import com.finlearn.quizservice.quizetl.domain.entity.Quiz;
import com.finlearn.quizservice.quizetl.domain.entity.QuizChoice;
import com.finlearn.quizservice.quizetl.domain.entity.QuizTopic;
import com.finlearn.quizservice.quizetl.domain.enums.TopicStatus;
import com.finlearn.quizservice.quizetl.domain.repository.CrawledSourceRepository;
import com.finlearn.quizservice.quizetl.domain.repository.QuizRepository;
import com.finlearn.quizservice.quizetl.domain.repository.QuizTopicRepository;
import com.finlearn.quizservice.quizetl.infrastructure.kafka.event.QuizETLCompletedEvent;
import com.finlearn.quizservice.quizsession.infrastructure.outbox.OutboxEvent;
import com.finlearn.quizservice.quizsession.infrastructure.outbox.OutboxEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class QuizGeneratorService {

    private final QuizTopicRepository quizTopicRepository;
    private final CrawledSourceRepository crawledSourceRepository;
    private final QuizRepository quizRepository;
    private final QuizGenerationLogService quizGenerationLogService;
    private final QuizDeduplicationService quizDeduplicationService;
    private final QuizQualityService quizQualityService;
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Value("${kafka.topics.quiz.etl-completed:finlearn-quiz-etl-completed}")
    private String topicEtlCompleted;

    public QuizGeneratorService(QuizTopicRepository quizTopicRepository,
            CrawledSourceRepository crawledSourceRepository, QuizRepository quizRepository,
            QuizGenerationLogService quizGenerationLogService, QuizDeduplicationService quizDeduplicationService,
            QuizQualityService quizQualityService, ChatClient.Builder chatClientBuilder, VectorStore vectorStore,
            OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate) {
        this.quizTopicRepository = quizTopicRepository;
        this.crawledSourceRepository = crawledSourceRepository;
        this.quizRepository = quizRepository;
        this.quizGenerationLogService = quizGenerationLogService;
        this.quizDeduplicationService = quizDeduplicationService;
        this.quizQualityService = quizQualityService;
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public record QuizAiResponse(String title, String question, List<QuizChoiceDto> choices, String answerExplanation) {
    }

    public record QuizChoiceDto(int no, String content, boolean correct) {
    }

    public void generateQuizzesFromEmbeddedTopics(UUID userId, String email, String role) {
        log.info("벡터화 완료된 모든 소주제들에 대한 퀴즈 생성을 시작합니다.");
        List<QuizTopic> embeddedTopics = quizTopicRepository.findByStatus(TopicStatus.EMBEDDED);
        log.info("퀴즈 생성 대기 중인 소주제 {}개", embeddedTopics.size());

        int successCount = 0;

        for (QuizTopic topic : embeddedTopics) {
            try {
                boolean success = generateQuizForTopic(topic);
                if (success) {
                    successCount++;
                }

                // 토픽 1개 처리당 AI API 호출이 최대 5회(임베딩 3회, 채팅 2회) 발생함.
                // 구글 제미나이 무료 티어 제한(15 RPM)을 방어하기 위해 15초간 넉넉히 대기 (1분에 4개 토픽 처리 -> 임베딩 12회, 채팅 8회로 안전)
                log.info("Rate Limit 방어를 위해 15초간 대기합니다...");
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                log.warn("퀴즈 생성 루프가 인터럽트되었습니다.");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("'{}' 퀴즈 생성 중 오류 발생: {}", topic.getSubTopic(), e.getMessage());
                
                // 트랜잭션 단위로 이 소주제의 상태를 FAILED로 격리 저장
                transactionTemplate.executeWithoutResult(status -> {
                    topic.updateStatus(TopicStatus.FAILED);
                    quizTopicRepository.save(topic);
                });
                
                quizGenerationLogService.saveFailureLog(topic.getMainTopic(), topic.getSubTopic(), null,
                        "오류 발생으로 격리 처리 및 다음 소주제 진행: " + e.getMessage());
            }
        }

        if (successCount > 0) {
            log.info("퀴즈 생성 배치 끝. 성공한 토픽이 있으므로 etl-completed 이벤트 outbox에 저장 시작");

            transactionTemplate.executeWithoutResult(status -> {
                QuizETLCompletedEvent event = new QuizETLCompletedEvent(userId, email, role, LocalDateTime.now());
                try {
                    String json = objectMapper.writeValueAsString(event);
                    outboxEventRepository.save(
                            OutboxEvent.create(topicEtlCompleted, userId != null ? userId.toString() : "SYSTEM", json));
                    log.info("etl-completed 이벤트 outbox에 저장 완료");
                } catch (JsonProcessingException e) {
                    throw new InternalServerException("이벤트 직렬화 실패: " + e.getMessage());
                }
            });
        } else {
            log.info("퀴즈 생성 배치 끝. 성공한 토픽이 없어 이벤트를 발행하지 않습니다.");
        }
    }

    @Transactional
    public boolean generateQuizForTopic(QuizTopic topic) {
        String keyword = topic.getSubTopic();
        log.info("'{}' 소주제에 대한 RAG 기반 퀴즈 생성을 시작합니다.", keyword);

        SearchRequest searchRequest = SearchRequest.builder().query(keyword).topK(3)
                .filterExpression("type == 'CONTENT' && subTopic == '" + keyword + "'").build();

        List<Document> docs = vectorStore.similaritySearch(searchRequest);
        String context = docs.stream().map(Document::getText).collect(Collectors.joining("\n\n"));

        if (context.isEmpty()) {
            log.warn("'{}' 소주제에 대한 검색 결과가 없어 퀴즈 생성을 건너뜁니다.", keyword);
            topic.updateStatus(TopicStatus.FAILED);
            quizTopicRepository.save(topic);
            return false;
        }

        String systemPrompt = """
                당신은 투자 초보자를 위한 최고의 금융 투자 교육 전문가입니다. 제공된 [문맥]의 내용을 바탕으로 투자 지식을 쉽게 학습할 수 있는 고품질 4지선다형 퀴즈를 만듭니다.

                요구사항:
                1. **초보자 눈높이**: 개념을 억지로 꼬아 어렵게 만들기보다, 투자 입문자가 핵심 투자 메커니즘을 확실하게 이해할 수 있도록 명확하고 쉬운 설명과 문장을 사용하세요.
                2. **투자 실전성**: 이 개념이 실제 주식, ETF, 혹은 선물 거래 상황에서 어떻게 작동하는지, 투자자에게 어떤 실질적 의미를 가지는지 이해를 돕는 직관적인 질문과 해설을 작성해 주세요.
                3. **자연스러운 문장**: 질문과 해설 모두에서 '제시된 문맥에 따르면', '지문에 의하면'과 같이 문맥 출처를 상투적으로 언급하는 표현은 절대 사용하지 마세요. 마치 원래 알고 있는 투자 상식을 풍부하게 설명하듯 자연스럽게 작성하세요.
                4. **표준 퀴즈 형식**: '다음 중 ...으로 옳은 것은?' 또는 '다음 상황에서 ...으로 가장 적절한 설명은?'과 같이 핵심 지식을 실전적으로 묻는 형식을 사용하세요.
                5. **해설**: 정답이 되는 이유뿐만 아니라, 오답들이 왜 틀렸는지 초보자의 관점에서 금융/투자 개념 위주로 친절하고 상세하게 풀어 설명해 주세요.
                """;

        String userPrompt = String.format("""
                아래 [키워드]와 [문맥]을 사용하여 퀴즈를 생성해 주세요.

                [키워드]
                %s

                [문맥]
                %s
                """, keyword, context);

        QuizAiResponse response = callChatWithRetry(systemPrompt, userPrompt);

        // 중복 체크
        if (quizDeduplicationService.isTooSimilarToExistingQuizzes(response.question())) {
            log.warn("생성된 퀴즈가 기존 문제와 너무 유사하여 저장하지 않습니다: {}", response.question());
            quizGenerationLogService.saveFailureLog(topic.getMainTopic(), keyword, response, "중복된 문제로 판명됨");
            topic.updateStatus(TopicStatus.FAILED);
            quizTopicRepository.save(topic);
            return false;
        }

        // 응답 데이터를 엔티티로 변환
        CrawledSource source = crawledSourceRepository.findByKeyword(keyword).orElse(null);
        Quiz quiz = Quiz.builder().quizTopicId(topic.getId()).crawledSourceId(source != null ? source.getId() : null)
                .title(response.title()).question(response.question()).answerExplanation(response.answerExplanation())
                .mainTopic(topic.getMainTopic()).subTopic(topic.getSubTopic())
                .choices(response.choices().stream()
                        .map(c -> QuizChoice.builder().no(c.no()).content(c.content()).correct(c.correct()).build())
                        .collect(Collectors.toList()))
                .build();

        // 품질 검사
        if (!quizQualityService.inspectQuizQuality(quiz, context)) {
            log.warn("'{}' 퀴즈가 품질 검사를 통과하지 못해 저장을 건너뜜.", keyword);
            quizGenerationLogService.saveFailureLog(topic.getMainTopic(), keyword, response, "AI 품질 검사 통과 실패");
            topic.updateStatus(TopicStatus.FAILED);
            quizTopicRepository.save(topic);
            return false;
        }

        quizRepository.save(quiz);
        quizGenerationLogService.saveSuccessLog(quiz);
        topic.updateStatus(TopicStatus.COMPLETED);
        quizTopicRepository.save(topic);
        log.info("'{}' 퀴즈 생성 및 품질 검사 완료, DB 저장 성공", keyword);

        // 중복 체크를 위해 퀴즈 질문도 벡터 DB에 저장
        vectorStore.accept(List.of(new Document(quiz.getQuestion(),
                Map.of("type", "QUIZ", "quizId", quiz.getId().toString(), "topicId", topic.getId().toString()))));
        return true;
    }

    private QuizAiResponse callChatWithRetry(String systemPrompt, String userPrompt) {
        int maxAttempts = 5;
        int delayMs = 60000;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return chatClient.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .call()
                        .entity(QuizAiResponse.class);
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
