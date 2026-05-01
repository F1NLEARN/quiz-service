package com.finlearn.quizservice.application.service;

import com.finlearn.quizservice.domain.entity.QuizTopic;
import com.finlearn.quizservice.domain.enums.MainTopic;
import com.finlearn.quizservice.domain.repository.QuizTopicRepository;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class QuizTopicGeneratorService {

    private final ChatClient chatClient;
    private final QuizTopicRepository quizTopicRepository;
    private final TransactionTemplate transactionTemplate;

    public QuizTopicGeneratorService(ChatClient.Builder chatClientBuilder, QuizTopicRepository quizTopicRepository,
            ChatMemory chatMemory, TransactionTemplate transactionTemplate) {
        this.chatClient = chatClientBuilder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.quizTopicRepository = quizTopicRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public record KeywordResponse(List<String> keywords) {
    }

    private static final int DEFAULT_TOPIC_COUNT = 10;

    public void generateAllTopicsKeywords() {
        generateAllTopicsKeywords(DEFAULT_TOPIC_COUNT);
    }

    public void generateAllTopicsKeywords(int count) {
        for (MainTopic topic : MainTopic.values()) {
            log.info("'{}' 대주제에 대한 소주제 생성 배치 시작", topic.getDescription());
            generateAndSaveTopics(topic, count);
        }
    }

    public List<String> generateAndSaveTopics(MainTopic mainTopic, int count) {
        log.info("{} 대주제에 대한 {}개의 소주제 생성", mainTopic.getDescription(), count);

        // 프롬프트 무작위성(Angle) 부여
        String[] angles = { "기초적이고 대중적인 필수 개념", "최신 금융 트렌드 및 이슈가 반영된 개념", "투자 전략 및 실무 분석과 관련된 개념",
                "투자 리스크, 함정, 주의사항과 관련된 개념", "실생활에서 바로 써먹을 수 있는 금융 상식", "뉴스에서 자주 나오는 경제 용어", "재테크 초보자가 흔히 하는 실수와 오해" };
        String selectedAngle = angles[new Random().nextInt(angles.length)];
        log.info("선택된 소주제 생성 방향: {}", selectedAngle);

        String prompt = String.format(
                "당신은 최고의 금융 교육 전문가입니다. " + "'%s' 대주제와 관련된, 사람들이 반드시 알아야 할 %s 위주로 핵심 경제 용어(명사형)를 %d개 추천해주세요. "
                        + "이 용어들은 한국어 위키피디아의 문서 제목으로 쓰일 수 있을 만큼 명확하고 보편적이어야 합니다. "
                        + "예를 들어 '주가수익률', '상장지수펀드', '복리' 등입니다. 부연 설명 없이 단어들만 배열로 주세요.",
                mainTopic.getDescription(), selectedAngle, count);

        // AI 호출
        KeywordResponse response = chatClient.prompt()
                .advisors(a -> a.param("chat_memory_conversation_id", mainTopic.name())).user(prompt).call()
                .entity(KeywordResponse.class);

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
}
