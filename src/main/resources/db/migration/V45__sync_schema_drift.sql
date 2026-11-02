-- ============================================================
-- V45: 固化两端数据库结构差异（correction_reason / 科研之星可空）
--
-- 背景：
--   `award_applications.correction_reason`、`career_plans.correction_reason`
--   以及 `award_research_stars.primary_category` 可空改造此前仅在本地库
--   手工执行，未纳入迁移版本。本脚本将其固化，保证新环境与远程库一致。
--
-- 说明：
--   本地库与远程库均已手工应用上述变更，故全部操作先判断存在性，
--   已应用时为空操作，可安全重复执行。
-- ============================================================

DROP PROCEDURE IF EXISTS sync_schema_drift;

DELIMITER $$

CREATE PROCEDURE sync_schema_drift()
BEGIN
    -- 1. award_applications 补充更正原因字段
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'award_applications'
          AND COLUMN_NAME = 'correction_reason'
    ) THEN
        ALTER TABLE `award_applications`
            ADD COLUMN `correction_reason` TEXT NULL DEFAULT NULL COMMENT '更正原因'
            AFTER `rejected_reason`;
    END IF;

    -- 2. career_plans 补充更正原因字段
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'career_plans'
          AND COLUMN_NAME = 'correction_reason'
    ) THEN
        ALTER TABLE `career_plans`
            ADD COLUMN `correction_reason` TEXT NULL DEFAULT NULL COMMENT '更正原因'
            AFTER `rejected_reason`;
    END IF;

    -- 3. award_research_stars.primary_category 允许为空
    --    先移除旧约束，再调整可空性，最后重建允许 NULL 的约束
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'award_research_stars'
          AND CONSTRAINT_NAME = 'ck_ars_primary_category'
          AND CONSTRAINT_TYPE = 'CHECK'
    ) THEN
        ALTER TABLE `award_research_stars`
            DROP CHECK `ck_ars_primary_category`;
    END IF;

    ALTER TABLE `award_research_stars`
        MODIFY COLUMN `primary_category` VARCHAR(50) NULL DEFAULT NULL COMMENT '主要成果类型';

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'award_research_stars'
          AND CONSTRAINT_NAME = 'ck_ars_primary_category'
          AND CONSTRAINT_TYPE = 'CHECK'
    ) THEN
        ALTER TABLE `award_research_stars`
            ADD CONSTRAINT `ck_ars_primary_category`
            CHECK (`primary_category` IS NULL
                   OR `primary_category` IN ('project', 'software_copyright', 'published_paper'));
    END IF;
END $$

DELIMITER ;

CALL sync_schema_drift();
DROP PROCEDURE sync_schema_drift;
