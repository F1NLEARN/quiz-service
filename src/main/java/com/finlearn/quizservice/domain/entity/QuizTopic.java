package com.finlearn.quizservice.domain.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.quizservice.domain.enums.MainTopic;
import com.finlearn.quizservice.domain.enums.TopicStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiz_topics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizTopic extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "quiz_topics_id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "main_topic", nullable = false)
    private MainTopic mainTopic;

    @Column(name = "sub_topic", nullable = false)
    private String subTopic;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TopicStatus status;

    @Builder
    public QuizTopic(MainTopic mainTopic, String subTopic) {
        this.mainTopic = mainTopic != null ? mainTopic : MainTopic.BASIC_FINANCE;
        this.subTopic = subTopic;
        this.status = TopicStatus.PENDING;
    }

    public void updateStatus(TopicStatus status) {
        this.status = status;
    }
}
