package com.example.studentarchives.event;

import com.example.studentarchives.entity.archive.Archive;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 档案审核通过事件。
 * <p>
 * 在档案状态由非通过（status != 2）变为通过（status = 2）后发布，
 * 用于触发下游的自动评分计算等逻辑。
 */
@Getter
@AllArgsConstructor
public class ArchiveApprovedEvent {

    /** 已审核通过的档案 */
    private final Archive archive;

    /** 执行审核操作的管理员用户 ID */
    private final Long operatorId;
}
