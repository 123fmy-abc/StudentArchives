package com.example.studentarchives.entity.ai;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ai_generation_logs")
public class AiGenerationLog extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "generation_type", length = 50, nullable = false)
    private String generationType;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "related_type", length = 100)
    private String relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "input_data", columnDefinition = "JSON")
    private String inputData;

    @Lob
    @Column(name = "output_content", columnDefinition = "TEXT", nullable = false)
    private String outputContent;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @Column(name = "token_usage")
    private Integer tokenUsage;

    @Column(name = "generation_time_ms")
    private Integer generationTimeMs;

    @Column(name = "call_status", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer callStatus = 1;

    @Lob
    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;

    @Column(name = "retry_of")
    private Long retryOf;

    @Column(name = "is_used", nullable = false, columnDefinition = "TINYINT")
    private Integer isUsed;

    @Column(name = "retention_until")
    private LocalDateTime retentionUntil;
}
