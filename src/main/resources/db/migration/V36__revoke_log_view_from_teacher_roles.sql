-- ============================================================
-- V36：日志仅管理员可见——回收非 admin 角色的 log:view 授权
--
-- 背景：V35（P0-2 修复）曾将 log:view 授予 teacher / counselor 角色，
-- 使教师端 /teacher/logs 可查日志。现需求变更为「日志仅能管理员查看」：
--   1. TeacherLogService 已改为 requireAdmin（代码层强制，历史遗留授权也不生效）；
--   2. 本迁移在数据层回收 log:view，保证角色权限面板与实际行为一致，
--      并防止自定义角色持有该权限码后经 /admin/logs 读接口越权查看。
--
-- role_permissions 含 deleted_at 软删除（BaseEntity），此处软删而非物理删除。
-- ============================================================

UPDATE `role_permissions` rp
JOIN `permissions` p ON p.`id` = rp.`permission_id` AND p.`deleted_at` IS NULL
JOIN `roles` r      ON r.`id` = rp.`role_id`       AND r.`deleted_at` IS NULL
SET rp.`deleted_at` = NOW()
WHERE rp.`deleted_at` IS NULL
  AND p.`code` = 'log:view'
  AND r.`code` <> 'admin';
