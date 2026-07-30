-- ============================================================
-- V14: Drop unused remember_token column from users table
-- remember_token 列已不再使用（记住我功能使用 JWT RefreshToken 实现）
-- ============================================================

-- 删除 users 表中未使用的 remember_token 列
ALTER TABLE `users` DROP COLUMN `remember_token`;
