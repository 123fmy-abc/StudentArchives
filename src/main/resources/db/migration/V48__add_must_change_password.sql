-- 新增：首次登录强制修改密码标记（AUTH-07 / BSR-07）
-- 存量账号默认 0（不强制），仅新建/导入/重置后的账号置 1，首次登录须改密后方可使用其他接口。
-- 幂等：users 表已存在该列时跳过，避免重复 ADD COLUMN 导致启动失败。

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'must_change_password'
);

SET @ddl := IF(
    @col_exists = 0,
    'ALTER TABLE `users` ADD COLUMN `must_change_password` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否需强制修改密码：1=首次登录须改密，0=否''',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
