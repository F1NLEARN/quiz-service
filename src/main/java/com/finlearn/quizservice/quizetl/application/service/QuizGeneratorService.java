package com.finlearn.quizservice.quizetl.application.service;

import com.finlearn.quizservice.quizetl.domain.entity.CrawledSource;
import com.finlearn.quizservice.quizetl.domain.entity.Quiz;
import com.finlearn.quizservice.quizetl.domain.entity.QuizChoice;
import com.finlearn.quizservice.quizetl.domain.entity.QuizTopic;
import com.finlearn.quizservice.quizetl.domain.enums.TopicStatus;
import com.finlearn.quizservice.quizetl.domain.repository.CrawledSourceRepository;
import com.finlearn.quizservice.quizetl.domain.repository.QuizRepository;
import com.finlearn.quizservice.quizetl.domain.repository.QuizTopicRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class QuizGeneratorService {

    private final QuizTopicRepository quizTopicRepository;
    private final CrawledSourceRepository crawledSourceRepository;
    private final QuizRepository quizRepository;
    private final QuizDeduplicationService quizDeduplicationService;
    private final QuizQualityService quizQualityService;
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public QuizGeneratorService(QuizTopicRepository quizTopicRepository,
            CrawledSourceRepository crawledSourceRepository, QuizRepository quizRepository,
            QuizDeduplicationService quizDeduplicationService, QuizQualityService quizQualityService,
            ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.quizTopicRepository = quizTopicRepository;
        this.crawledSourceRepository = crawledSourceRepository;
        this.quizRepository = quizRepository;
        this.quizDeduplicationService = quizDeduplicationService;
        this.quizQualityService = quizQualityService;
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    public record QuizAiResponse(String title, String question, List<QuizChoiceDto> choices, String answerExplanation) {
    }

    public record QuizChoiceDto(int no, String content, boolean correct) {
    }

    public void generateQuizzesFromEmbeddedTopics() {
        log.info("벡터화 완료된 모든 소주제들에 대한 퀴즈 생성을 시작합니다.");
        List<QuizTopic> embeddedTopics = quizTopicRepository.findByStatus(TopicStatus.EMBEDDED);
        log.info("퀴즈 생성 대기 중인 소주제 {}개", embeddedTopics.size());

        for (QuizTopic topic : embeddedTopics) {
            try {
                generateQuizForTopic(topic);
                topic.updateStatus(TopicStatus.COMPLETED);
                quizTopicRepository.save(topic);

                // Rate Limit 1분에 15회 있음
                log.info("Rate Limit 대응을 위해 5초간 대기합니다...");
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.warn("퀴즈 생성 루프가 인터럽트되었습니다.");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("'{}' 소주제 퀴즈 생성 중 오류 발생: {}", topic.getSubTopic(), e.getMessage());
                // 에러 발생 시에도 할당량 회복을 위해 잠시 대기
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    @Transactional
    public void generateQuizForTopic(QuizTopic topic) {
        String keyword = topic.getSubTopic();
        log.info("'{}' 소주제에 대한 RAG 기반 퀴즈 생성을 시작합니다.", keyword);

        SearchRequest searchRequest = SearchRequest.builder().query(keyword).topK(3)
                .filterExpression("type == 'CONTENT' && subTopic == '" + keyword + "'").build();

        List<Document> docs = vectorStore.similaritySearch(searchRequest);
        String context = docs.stream().map(Document::getText).collect(Collectors.joining("\n\n"));

        if (context.isEmpty()) {
            log.warn("'{}' 소주제에 대한 검색 결과가 없어 퀴즈 생성을 건너뜁니다.", keyword);
            return;
        }

        String systemPrompt = """
                당신은 금융 교육 전문가입니다. 제공된 [문맥]의 내용을 바탕으로 금융 지식을 테스트하는 고품질 4지선다형 퀴즈를 만듭니다.

                요구사항:
                1. **자연스러운 문장**: 질문과 해설 모두에서 '제시된 문맥에 따르면', '지문에 의하면', '제공된 자료에서는'과 같이 출처를 언급하는 상투적인 서두나 표현을 절대 사용하지 마세요.
                2. **직접적인 지식 전달**: 마치 원래 알고 있는 금융 지식을 설명하듯 자연스럽고 권위 있는 어조로 작성하세요.
                3. **표준 퀴즈 형식**: '다음 중 ...으로 옳은 것은?'과 같이 핵심 지식을 직접 묻는 형식을 사용하세요.
                4. **내용의 질**: 정답은 문맥 내에서 명확히 도출되어야 하며, 오답(Distractors)은 매력적이고 논리적이어야 합니다.
                5. **해설**: 정답의 근거와 오답이 틀린 이유를 금융 개념 위주로 상세히 설명하세요.
                """;

        String userPrompt = String.format("""
                아래 [키워드]와 [문맥]을 사용하여 퀴즈를 생성해 주세요.

                [키워드]
                %s

                [문맥]
                %s
                """, keyword, context);

        QuizAiResponse response = chatClient.prompt().system(systemPrompt).user(userPrompt).call()
                .entity(QuizAiResponse.class);

        // 중복 체크
        if (quizDeduplicationService.isTooSimilarToExistingQuizzes(response.question())) {
            log.warn("생성된 퀴즈가 기존 문제와 너무 유사하여 저장하지 않습니다: {}", response.question());
            return;
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
            log.warn("'{}' 퀴즈가 품질 검사를 통과하지 못해 저장을 건너뜁니다.", keyword);
            return;
        }

        quizRepository.save(quiz);
        log.info("'{}' 퀴즈 생성 및 품질 검사 완료, DB 저장 성공", keyword);

        // 중복 체크를 위해 퀴즈 질문도 벡터 DB에 저장
        vectorStore.accept(List.of(new Document(quiz.getQuestion(),
                Map.of("type", "QUIZ", "quizId", quiz.getId().toString(), "topicId", topic.getId().toString()))));
    }
}
