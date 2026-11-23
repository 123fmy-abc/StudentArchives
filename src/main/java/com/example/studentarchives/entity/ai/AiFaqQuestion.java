package com.example.studentarchives.entity.ai;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "ai_faq_questions")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class AiFaqQuestion extends BaseEntity {

    @Column(name = "question", nullable = false, length = 200)
    private String question;

    @Column(name = "sort", nullable = false)
    private Integer sort;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
