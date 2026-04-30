package com.finlearn.quizservice.domain.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.quizservice.domain.enums.MainTopic;
import com.finlearn.quizservice.domain.enums.QuizGenerationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "quiz_generation_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizGenerationLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "quiz_generation_logs_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "crawled_sources_id", nullable = false)
    private UUID crawledSourceId;

    @Column(name = "quizzes_id", nullable = false)
    private UUID quizId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QuizGenerationStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "quiz_title", columnDefinition = "TEXT", nullable = false)
    private String quizTitle;

    @Column(name = "quiz_question", columnDefinition = "TEXT", nullable = false)
    private String quizQuestion;

    @Column(name = "quiz_answer_explanation", columnDefinition = "TEXT", nullable = false)
    private String quizAnswerExplanation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quiz_choices", columnDefinition = "json", nullable = false)
    private List<QuizChoice> quizChoices;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_main_topic", nullable = false)
    private MainTopic quizMainTopic;

    @Column(name = "quiz_sub_topic", nullable = false)
    private String quizSubTopic;

    @Builder
    public QuizGenerationLog(UUID crawledSourceId, UUID quizId, QuizGenerationStatus status, 
                             String errorMessage, String quizTitle, String quizQuestion, 
                             String quizAnswerExplanation, List<QuizChoice> quizChoices, 
                             MainTopic quizMainTopic, String quizSubTopic) {
        this.crawledSourceId = crawledSourceId;
        this.quizId = quizId;
        this.status = status != null ? status : QuizGenerationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.quizTitle = quizTitle;
        this.quizQuestion = quizQuestion;
        this.quizAnswerExplanation = quizAnswerExplanation;
        this.quizChoices = quizChoices;
        this.quizMainTopic = quizMainTopic;
        this.quizSubTopic = quizSubTopic;
    }
}
