package com.blift.aiservice.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Stores the nightly operational context snapshot for each subscribed RCIC.
 * The snapshot_json field contains all SQL-derived facts used to generate the AI briefing.
 * No PII or LLM output is stored here — only aggregated operational data.
 */
@Entity
@Table(name = "rcic_ai_context",
        uniqueConstraints = @UniqueConstraint(name = "uq_rcic_context_date", columnNames = {"rcic_user_id", "context_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RcicAiContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rcic_user_id", nullable = false)
    private Long rcicUserId;

    @Column(name = "context_date", nullable = false)
    private LocalDate contextDate;

    @Type(JsonBinaryType.class)
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "jsonb")
    private String snapshotJson;

    @CreationTimestamp
    @Column(name = "built_at", nullable = false, updatable = false)
    private Instant builtAt;

    /** Track success/failure per build for retry logic */
    @Column(name = "build_status", length = 20)
    private String buildStatus;

    @Column(name = "build_error_message", columnDefinition = "TEXT")
    private String buildErrorMessage;
}
