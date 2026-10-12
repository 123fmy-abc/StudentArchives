package com.example.studentarchives.entity.org;

import com.example.studentarchives.entity.BaseEntityNoUpdate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 学期批量导入记录（append-only，无软删除/更新）。
 * <p>
 * 对应《管理端接口文档》九、学期管理模块 9.6 批量导入的落库记录，
 * 供「本次导入结果」回看历史批次（GET /admin/semesters/import-records）。
 */
@Getter
@Setter
@Entity
@Table(name = "semester_import_records")
public class SemesterImportRecord extends BaseEntityNoUpdate {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    @Column(name = "fail_count", nullable = false)
    private Integer failCount;

    /** 失败明细（JSON 数组，元素结构见 SemesterManageService.SemesterImportFailItem） */
    @Column(name = "fail_details", columnDefinition = "JSON")
    private String failDetails;
}
