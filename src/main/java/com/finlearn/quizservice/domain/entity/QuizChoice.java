package com.finlearn.quizservice.domain.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizChoice {
    private int no;
    private String content;
    @JsonProperty("isCorrect")
    private boolean isCorrect;
}
