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

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "generation_type")
    private String generationType;

    @Column(name = "input_data", columnDefinition = "JSON")
    private String inputData;

    @Lob
    @Column(name = "output_content", columnDefinition = "TEXT")
    private String outputContent;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "token_usage")
    private Integer tokenUsage;

    @Column(name = "generation_time_ms")
    private Integer generationTimeMs;

    @Column(name = "is_used")
    private Integer isUsed;
}
