package com.example.studentarchives.entity.archive;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "archive_book_reviews")
public class ArchiveBookReview extends BaseEntity {

    @Column(name = "archive_id", nullable = false)
    private Long archiveId;

    @Column(name = "book_name", nullable = false, length = 255)
    private String bookName;

    @Column(name = "read_month", length = 20)
    private String readMonth;

    @Lob
    @Column(name = "review_content", nullable = false, columnDefinition = "TEXT")
    private String reviewContent;
}
