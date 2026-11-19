-- ============================================================
-- V57: 评选说明配置改为「每所学校自主配置」
--
-- 背景：
--   评选说明（archive_type_configs / award_type_configs 的 evaluate_* 字段）
--   此前为全局配置，所有学校共用同一份文案，无法按学校差异化维护。
--   本迁移为两表增加 school_id，并把已有全局配置复制到每所学校作为初始文案，
--   之后由各校管理员通过 /admin/guide-configs 独立维护。
--
-- 兼容策略：
--   1. 已有配置归属「默认学校」（schools 中未删除且 id 最小的学校）；
--   2. 其余学校复制一份同样的配置作为初始文案，可各自修改；
--   3. 复制使用 NOT EXISTS 去重，脚本可重复执行；
--   4. 新增学校不自动落库配置，管理端首次保存时自动创建（学生端在配置前返回空说明）。
--
-- 权限：
--   新增权限码 guide:manage，授予 admin 角色（admin 角色在 AdminAuthService 中直通）。
-- ============================================================

-- ------------------------------------------------------------
-- 1. archive_type_configs —— 增加 school_id
-- ------------------------------------------------------------
ALTER TABLE `archive_type_configs`
    ADD COLUMN `school_id` BIGINT UNSIGNED NULL COMMENT '所属学校 schools.id' AFTER `id`;

UPDATE `archive_type_configs`
SET `school_id` = COALESCE((SELECT MIN(s.`id`) FROM `schools` s WHERE s.`deleted_at` IS NULL), 1)
WHERE `school_id` IS NULL;

INSERT INTO `archive_type_configs`
(`school_id`, `archive_type`, `type_name`, `evaluate_desc`, `evaluate_requirements`, `evaluate_notes`,
 `apply_desc`, `icon`, `sort`, `status`)
SELECT s.`id`, g.`archive_type`, g.`type_name`, g.`evaluate_desc`, g.`evaluate_requirements`,
       g.`evaluate_notes`, g.`apply_desc`, g.`icon`, g.`sort`, g.`status`
FROM `schools` s
JOIN `archive_type_configs` g
  ON g.`school_id` = COALESCE((SELECT MIN(s2.`id`) FROM `schools` s2 WHERE s2.`deleted_at` IS NULL), 1)
 AND g.`deleted_at` IS NULL
WHERE s.`deleted_at` IS NULL
  AND s.`id` <> g.`school_id`
  AND NOT EXISTS (
      SELECT 1
      FROM `archive_type_configs` x
      WHERE x.`school_id` = s.`id`
        AND x.`archive_type` = g.`archive_type`
        AND x.`deleted_at` IS NULL
  );

-- 说明：不建 schools 物理外键。空库首次启动时 Flyway 先于 sql/ 种子脚本执行，
-- schools 可能暂无数据，此时建外键会因回填值无对应学校而失败；
-- school_id 的引用完整性由应用层（schools 删除不做物理删除）保证。
ALTER TABLE `archive_type_configs`
    MODIFY COLUMN `school_id` BIGINT UNSIGNED NOT NULL COMMENT '所属学校 schools.id',
    DROP INDEX `uk_atc_archive_type`,
    ADD UNIQUE KEY `uk_atc_school_archive_type` (`school_id`, `archive_type`, `is_deleted_null`),
    ADD KEY `idx_atc_school_id` (`school_id`);

-- ------------------------------------------------------------
-- 2. award_type_configs —— 增加 school_id
-- ------------------------------------------------------------
ALTER TABLE `award_type_configs`
    ADD COLUMN `school_id` BIGINT UNSIGNED NULL COMMENT '所属学校 schools.id' AFTER `id`;

UPDATE `award_type_configs`
SET `school_id` = COALESCE((SELECT MIN(s.`id`) FROM `schools` s WHERE s.`deleted_at` IS NULL), 1)
WHERE `school_id` IS NULL;

INSERT INTO `award_type_configs`
(`school_id`, `award_type`, `type_name`, `evaluate_desc`, `evaluate_requirements`, `evaluate_notes`,
 `apply_desc`, `icon`, `sort`, `status`)
SELECT s.`id`, g.`award_type`, g.`type_name`, g.`evaluate_desc`, g.`evaluate_requirements`,
       g.`evaluate_notes`, g.`apply_desc`, g.`icon`, g.`sort`, g.`status`
FROM `schools` s
JOIN `award_type_configs` g
  ON g.`school_id` = COALESCE((SELECT MIN(s2.`id`) FROM `schools` s2 WHERE s2.`deleted_at` IS NULL), 1)
 AND g.`deleted_at` IS NULL
WHERE s.`deleted_at` IS NULL
  AND s.`id` <> g.`school_id`
  AND NOT EXISTS (
      SELECT 1
      FROM `award_type_configs` x
      WHERE x.`school_id` = s.`id`
        AND x.`award_type` = g.`award_type`
        AND x.`deleted_at` IS NULL
  );

-- 说明：同 archive 侧，不建 schools 物理外键，避免空库初始化时 schools 尚无数据导致迁移失败。
ALTER TABLE `award_type_configs`
    MODIFY COLUMN `school_id` BIGINT UNSIGNED NOT NULL COMMENT '所属学校 schools.id',
    DROP INDEX `uk_atc_award_type`,
    ADD UNIQUE KEY `uk_awtc_school_award_type` (`school_id`, `award_type`, `is_deleted_null`),
    ADD KEY `idx_awtc_school_id` (`school_id`);

-- ------------------------------------------------------------
-- 3. 权限码 guide:manage
-- ------------------------------------------------------------
INSERT IGNORE INTO `permissions` (`name`, `code`, `type`, `parent_id`, `sort`, `status`)
SELECT '评选说明配置', 'guide:manage', 3, NULL, 8, 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `permissions` p WHERE p.`code` = 'guide:manage' AND p.`deleted_at` IS NULL
);

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `roles` r
JOIN `permissions` p ON p.`code` = 'guide:manage' AND p.`deleted_at` IS NULL
WHERE r.`deleted_at` IS NULL
  AND r.`code` = 'admin';
