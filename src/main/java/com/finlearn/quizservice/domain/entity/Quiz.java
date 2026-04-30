package com.finlearn.quizservice.domain.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.quizservice.domain.enums.MainTopic;
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
@Table(name = "quizzes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quiz extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "quizzes_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "quiz_topics_id", nullable = false)
    private UUID quizTopicId;

    @Column(name = "title", columnDefinition = "TEXT", nullable = false)
    private String title;

    @Column(name = "question", columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(name = "answer_explanation", columnDefinition = "TEXT", nullable = false)
    private String answerExplanation;

    @Column(name = "crawled_sources_id")
    private UUID crawledSourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "choices", columnDefinition = "json", nullable = false)
    private List<QuizChoice> choices;

    @Enumerated(EnumType.STRING)
    @Column(name = "main_topic", nullable = false)
    private MainTopic mainTopic;

    @Column(name = "sub_topic", nullable = false)
    private String subTopic;

    @Builder
    public Quiz(UUID quizTopicId, String title, String question, String answerExplanation, 
                UUID crawledSourceId, List<QuizChoice> choices, MainTopic mainTopic, String subTopic) {
        this.quizTopicId = quizTopicId;
        this.title = title;
        this.question = question;
        this.answerExplanation = answerExplanation;
        this.crawledSourceId = crawledSourceId;
        this.choices = choices;
        this.mainTopic = mainTopic != null ? mainTopic : MainTopic.BASIC_FINANCE;
        this.subTopic = subTopic;
    }
}
