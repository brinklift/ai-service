package com.blift.aiservice.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Stores the LLM-generated daily briefing for each subscribed RCIC.
 * This is the AI output — cached and served instantly on page load.
 * client_spotlights stores per-client pathway assessment (CRS estimate + match label).
 */
@Entity
@Table(name = "rcic_ai_briefing",
        uniqueConstraints = @UniqueConstraint(name = "uq_rcic_briefing_date", columnNames = {"rcic_user_id", "briefing_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RcicAiBriefing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rcic_user_id", nullable = false)
    private Long rcicUserId;

    @Column(name = "briefing_date", nullable = false)
    private LocalDate briefingDate;

    @Column(name = "briefing_text", nullable = false, columnDefinition = "TEXT")
    private String briefingText;

    @Type(JsonBinaryType.class)
    @Column(name = "bullet_points", columnDefinition = "jsonb")
    private String bulletPoints;

    @Type(JsonBinaryType.class)
    @Column(name = "client_spotlights", columnDefinition = "jsonb")
    private String clientSpotlights;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "context_snapshot_id")
    private RcicAiContext contextSnapshot;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;
}
