-- V59: AI 助手常见问题（FAQ）配置
--
-- 背景
--   AI 助手聊天页底部展示"常见问题"快捷入口，此前为前端硬编码。
--   现改为由管理员在管理端自主维护（增删改、排序、启用/禁用），
--   学生端通过 GET /ai/faqs 拉取启用中的问题列表，点击后作为消息
--   内容走现有 AI 对话发送链路，无需改动对话逻辑。
--
-- 1. ai_faq_questions — AI 常见问题配置表

CREATE TABLE `ai_faq_questions` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `question`         VARCHAR(200)    NOT NULL COMMENT '常见问题内容（点击后作为消息发送）',
    `sort`             INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    `status`           INT             NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_afq_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI助手常见问题配置表';

-- 2. 默认常见问题（幂等：固定 ID，可重复执行）

INSERT IGNORE INTO `ai_faq_questions` (`id`, `question`, `sort`, `status`) VALUES
(1, '帮我总结我的大一生活', 1, 1),
(2, '我未来适合做什么职业', 2, 1),
(3, '我下学期应该多往哪方面发展', 3, 1);

-- 3. 管理端权限码 ai:faq:manage（admin 角色在 AdminAuthService 中直接放行，
--    此处显式授予 admin 角色，便于权限管理界面展示）

INSERT IGNORE INTO `permissions` (`name`, `code`, `type`, `parent_id`, `sort`, `status`)
SELECT 'AI常见问题配置', 'ai:faq:manage', 3, NULL, 9, 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `permissions` p WHERE p.`code` = 'ai:faq:manage' AND p.`deleted_at` IS NULL
);

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `roles` r
JOIN `permissions` p ON p.`code` = 'ai:faq:manage' AND p.`deleted_at` IS NULL
WHERE r.`deleted_at` IS NULL
  AND r.`code` = 'admin';
