package com.example.studentarchives.entity.file;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 通用附件关联实体（多态）
 * <p>
 * 通过 biz_type + biz_id 关联不同业务记录，替代 archive_attachments、
 * award_attachments、career_action_files 等分散的附件表。
 */
@Getter
@Setter
@Entity
@Table(name = "file_uploads")
public class AttachmentRelation extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "biz_type", length = 50)
    private String bizType;

    @Column(name = "biz_id")
    private Long bizId;

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

    @Column(name = "preview_url", length = 500)
    private String previewUrl;

    @Column(name = "convert_status", nullable = false)
    private Integer convertStatus;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Column(name = "file_status", nullable = false)
    private Integer fileStatus;

    @Column(name = "temp_expire_at")
    private LocalDateTime tempExpireAt;

    @Column(name = "download_expire_at")
    private LocalDateTime downloadExpireAt;
}
