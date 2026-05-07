package com.finlearn.quizservice.quizetl.domain.entity;

import com.finlearn.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "crawled_sources")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawledSource extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "crawled_sources_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "raw_response", columnDefinition = "TEXT", nullable = false)
    private String rawResponse;

    @Column(name = "keyword", nullable = false)
    private String keyword;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Builder
    public CrawledSource(String sourceUrl, String rawResponse, String keyword, String title, String content) {
        this.sourceUrl = sourceUrl;
        this.rawResponse = rawResponse;
        this.keyword = keyword;
        this.title = title;
        this.content = content;
    }
}
