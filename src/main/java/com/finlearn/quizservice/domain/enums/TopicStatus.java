package com.finlearn.quizservice.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TopicStatus {
    PENDING("수집 아직 안함"), CRAWLED("수집 완료"), NOT_FOUND("문서 없음");

    private final String description;
}
