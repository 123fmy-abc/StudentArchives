package com.example.studentarchives.entity.log;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ai_generation_logs")
public class AiGenerationLog extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "generation_type", nullable = false, length = 50)
    private String generationType;

    @Column(name = "input_data", nullable = false, columnDefinition = "JSON")
    private String inputData;

    @Lob
    @Column(name = "output_content", nullable = false, columnDefinition = "TEXT")
    private String outputContent;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "token_usage")
    private Integer tokenUsage;

    @Column(name = "generation_time_ms")
    private Integer generationTimeMs;

    @Column(name = "is_used", nullable = false)
    private byte isUsed;
}
