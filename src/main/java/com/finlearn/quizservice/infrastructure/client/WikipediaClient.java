package com.finlearn.quizservice.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class WikipediaClient {

    private final RestClient restClient;

    public WikipediaClient(@Value("${wiki.api.base-url}") String baseUrl,
            @Value("${wiki.api.contact-email}") String contactEmail) {
        log.info("WikipediaClient 초기화 - URL: {}, 연락처: {}", baseUrl, contactEmail);
        this.restClient = RestClient.builder().baseUrl(baseUrl)
                .defaultHeader("User-Agent", "F1NLEARN-QuizBot/1.0 (" + contactEmail + ")").build();
    }

    public String fetchArticleContent(String keyword) {
        log.info("위키피디아에서 '{}' 문서의 본문 수집 시작", keyword);

        try {
            JsonNode response = restClient.get().uri(uriBuilder -> uriBuilder/* 데이터 줘라 */.queryParam("action", "query")
                    /* 본문(extracts) 줘라 */.queryParam("prop", "extracts")
                    /* HTML 태그 없는 순수 텍스트로 줘라 */.queryParam("explaintext", "1")/* 검색어 */.queryParam("titles", keyword)
                    /* 응답은 JSON으로 */.queryParam("format", "json").build()).retrieve().body(JsonNode.class);

            if (response != null && response.has("query") && response.get("query").has("pages")) {
                JsonNode pages = response.get("query").get("pages");
                if (pages.properties().iterator().hasNext()) {
                    JsonNode page = pages.properties().iterator().next().getValue();

                    if (page.has("missing")) {
                        log.warn("'{}' 문서를 찾을 수 없습니다.", keyword);
                        return null;
                    }

                    if (page.has("extract")) {
                        String extract = page.get("extract").asText();
                        log.info("'{}' 문서 수집 완료 ({} 글자)", keyword, extract.length());
                        return extract;
                    }
                }
            }
        } catch (Exception e) {
            log.error("위키피디아 API 호출 중 오류 발생: {}", e.getMessage());
        }

        log.warn("'{}' 문서에서 본문을 추출하지 못했습니다.", keyword);
        return null;
    }
}
