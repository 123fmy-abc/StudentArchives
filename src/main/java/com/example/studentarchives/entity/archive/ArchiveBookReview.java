package com.example.studentarchives.entity.archive;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "archive_book_reviews")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class ArchiveBookReview extends BaseEntity {

    @Column(name = "archive_id", nullable = false)
    private Long archiveId;

    @Column(name = "book_name", nullable = false, length = 255)
    private String bookName;

    @Column(name = "read_month")
    private LocalDate readMonth;

    @Lob
    @Column(name = "review_content", columnDefinition = "TEXT")
    private String reviewContent;
}
