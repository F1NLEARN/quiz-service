package com.finlearn.quizservice.quizetl.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.finlearn.quizservice.quizetl.application.port.out.WikipediaPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class WikipediaClient implements WikipediaPort {

    private final RestClient restClient;
    private final String webUrlPrefix;

    public WikipediaClient(@Value("${wiki.api.base-url}") String baseUrl,
            @Value("${wiki.api.contact-email}") String contactEmail,
            @Value("${wiki.web-url-prefix}") String webUrlPrefix) {
        this.restClient = RestClient.builder().baseUrl(baseUrl)
                .defaultHeader("User-Agent", "F1NLEARN-QuizBot/1.0 (" + contactEmail + ")").build();
        this.webUrlPrefix = webUrlPrefix;
    }

    @Override
    public String fetchArticleContent(String keyword) {
        log.info("위키피디아에서 '{}' 문서의 본문 수집 시작", keyword);

        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder/* 데이터 줘라 */.queryParam("action", "query")
                            /* 본문(extracts) 줘라 */.queryParam("prop", "extracts")
                            /* HTML 태그 없는 순수 텍스트로 줘라 */.queryParam("explaintext", "1")
                            /* 동의어면 자동 이동해라 */.queryParam("redirects", "1")/* 검색어 */.queryParam("titles", keyword)
                            /* 응답은 JSON으로 */.queryParam("format", "json").build())
                    .retrieve().body(JsonNode.class);

            log.debug("위키피디아 API 응답 결과: {}", response);

            // 비정상 응답
            if (response == null || !response.has("query") || !response.get("query").has("pages")
                    || response.get("query").get("pages").isEmpty()
                    || response.get("query").get("pages").elements().next().has("missing")) {
                log.warn("위키피디아에서 '{}' 문서를 수집 불가 (데이터 없음)", keyword);
                return null;
            }

            // 정상 응답
            JsonNode firstPage = response.get("query").get("pages").elements().next();
            if (firstPage.has("extract")) {
                return firstPage.get("extract").asText();
            }

        } catch (Exception e) {
            log.error("위키피디아 API 호출 중 네트워크/인프라 오류 발생: {}", e.getMessage());
            throw new RuntimeException("Wikipedia API 호출 실패: " + keyword, e);
        }

        return null;
    }

    @Override
    public String generateArticleUrl(String keyword) {
        return webUrlPrefix + keyword.replace(" ", "_");
    }
}
