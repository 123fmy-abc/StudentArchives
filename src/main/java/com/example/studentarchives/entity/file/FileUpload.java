package com.example.studentarchives.entity.file;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "file_uploads")
public class FileUpload extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "fileable_type", nullable = false, length = 50)
    private String fileableType;

    @Column(name = "fileable_id", nullable = false)
    private Long fileableId;

    @Column(name = "file_category", length = 50)
    private String fileCategory;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "disk", nullable = false, length = 50)
    private String disk;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
