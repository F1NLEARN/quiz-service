package com.finlearn.quizservice.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MainTopic {
    DOMESTIC_STOCK("국내 주식"), DOMESTIC_ETF("국내 ETF"), FUTURES("선물"), BASIC_FINANCE("기초 금융");

    private final String description;
}
