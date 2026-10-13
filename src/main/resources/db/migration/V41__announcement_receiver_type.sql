-- ============================================================
-- V41: announcements 增加 receiver_type（接收对象维度）
-- 与 target_type（接收范围：all/college/major/class）正交，
-- receiver_type 决定「发给谁」：all/student/staff/admin
-- 旧数据默认 'all'，保持「学生 + 教职工」都收的既有行为。
-- ============================================================

ALTER TABLE `announcements`
    ADD COLUMN `receiver_type` VARCHAR(20) NOT NULL DEFAULT 'all' COMMENT '接收对象：all/student/staff/admin' AFTER `target_id`;