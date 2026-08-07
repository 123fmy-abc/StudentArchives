-- ============================================================
-- V23: 将学历层次字段从班级表迁移到学生档案表
-- ============================================================

-- 老环境（classes 表曾存在 degree_type 字段）：按学生所在班级回填学历层次到个人档案
-- 新环境（classes 表无该字段）：自动跳过，保证全新数据库可正常建库
SET @has_class_degree = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'classes'
      AND COLUMN_NAME = 'degree_type'
);

-- 回填：仅老环境需要
SET @backfill_sql = IF(@has_class_degree > 0,
    'UPDATE student_profiles p JOIN classes c ON p.class_id = c.id SET p.degree_type = c.degree_type WHERE p.degree_type IS NULL AND c.degree_type IS NOT NULL',
    'SELECT 1');
PREPARE backfill_stmt FROM @backfill_sql;
EXECUTE backfill_stmt;
DEALLOCATE PREPARE backfill_stmt;

-- 清理班级表字段：MySQL 8 不支持 DROP COLUMN IF EXISTS，故用预处理语句动态执行
SET @drop_sql = IF(@has_class_degree > 0,
    'ALTER TABLE classes DROP COLUMN degree_type',
    'SELECT 1');
PREPARE drop_stmt FROM @drop_sql;
EXECUTE drop_stmt;
DEALLOCATE PREPARE drop_stmt;
