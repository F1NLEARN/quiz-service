package com.finlearn.quizservice.quizetl.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class QuizTopicCreatedEvent {
    private UUID userId;
    private String email;
    private String role;
    private LocalDateTime createdAt;
}
