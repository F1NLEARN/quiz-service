package com.finlearn.quizservice.quizetl.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TopicStatus {
    PENDING("수집 아직 안함"), CRAWLED("수집 완료"), NOT_FOUND("문서 없음"), EMBEDDED("벡터화 완료"), COMPLETED("퀴즈 생성 완료"),
    FAILED("처리 실패");

    private final String description;
}
