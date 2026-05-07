package com.finlearn.quizservice.quizetl.application.service;

import com.finlearn.quizservice.quizetl.domain.entity.Quiz;
import com.finlearn.quizservice.quizetl.domain.entity.QuizChoice;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QuizQualityService {

    private final ChatClient chatClient;

    public QuizQualityService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public record QualityReviewResponse(boolean passed, String reason) {
    }

    public boolean inspectQuizQuality(Quiz quiz, String context) {
        log.info("퀴즈 품질 검사를 시작합니다: {}", quiz.getTitle());

        // 기초 검사
        if (!runRuleBasedValidation(quiz)) {
            return false;
        }

        // AI 자가 검사
        return runAiSelfReview(quiz, context);
    }

    private boolean runRuleBasedValidation(Quiz quiz) {
        List<QuizChoice> choices = quiz.getChoices();

        if (choices == null || choices.size() != 4) {
            log.warn("선택지 개수 오류");
            return false;
        }

        long correctCount = choices.stream().filter(QuizChoice::isCorrect).count();
        if (correctCount != 1) {
            log.warn("정답 개수 오류");
            return false;
        }

        if (quiz.getQuestion() == null || quiz.getQuestion().isBlank() || quiz.getAnswerExplanation() == null
                || quiz.getAnswerExplanation().isBlank()) {
            log.warn("필수 필드 누락");
            return false;
        }

        log.info("기초 검사 통과");
        return true;
    }

    private boolean runAiSelfReview(Quiz quiz, String context) {
        String choicesText = quiz.getChoices().stream()
                .map(c -> String.format("%d. %s (정답여부: %b)", c.getNo(), c.getContent(), c.isCorrect()))
                .reduce((a, b) -> a + "\n" + b).orElse("");

        String systemPrompt = """
                당신은 금융 교육 콘텐츠 감사관입니다. 제공된 [문맥]을 바탕으로 작성된 [퀴즈]의 품질을 엄격히 검토해주세요.

                검토 항목:
                1. 문맥상 정답이 확실하고 논리적인가?
                2. 문제 자체에 오류(중복 정답, 정답 없음, 사실 관계 오류 등)가 없는가?
                3. 선택지가 서로 명확히 구별되어 혼동을 주지 않는가?
                4. **이 퀴즈의 내용이 제시된 [대주제]와 실질적으로 관련이 있는 금융/경제 지식인가?** 만약 가수, 연예인, 무관한 일반 상식 등의 내용이라면 반드시 탈락(passed: false)시켜주세요.

                모든 항목을 통과하면 passed를 true로, 하나라도 문제가 있다면 false로 응답하고 구체적인 이유(reason)를 적어주세요.
                """;

        String userPrompt = String.format("""
                아래 데이터를 바탕으로 퀴즈의 품질을 검토해 주세요.

                [대주제]
                %s

                [문맥]
                %s

                [퀴즈]
                질문: %s
                선택지:
                %s
                정답 해설: %s
                """, quiz.getMainTopic().getDescription(), context, quiz.getQuestion(), choicesText,
                quiz.getAnswerExplanation());

        try {
            QualityReviewResponse response = chatClient.prompt().system(systemPrompt).user(userPrompt).call()
                    .entity(QualityReviewResponse.class);

            if (response.passed()) {
                log.info("AI 자가 검사 통과");
                return true;
            } else {
                log.warn("AI 자가 검사 실패: {}", response.reason());
                return false;
            }
        } catch (Exception e) {
            log.error("AI 자가 검사 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }
}
