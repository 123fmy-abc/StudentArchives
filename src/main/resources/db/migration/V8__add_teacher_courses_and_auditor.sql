-- ============================================================================
-- V8: 教师课程、审核员角色、权限扩展
-- 描述：增加 teacher_courses 表、审核员角色、教师端权限、档案课程关联、视图
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 8.1 教师授课关系表
-- 课任教师需要关联授课班级和课程，记录教师与授课班级+课程+学期的对应关系
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS teacher_courses (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    school_id       BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    teacher_id      BIGINT UNSIGNED NOT NULL COMMENT '教师用户ID，关联 users.id',
    class_id        BIGINT UNSIGNED NOT NULL COMMENT '关联 classes.id',
    course_code     VARCHAR(50)     NOT NULL COMMENT '课程编码',
    course_name     VARCHAR(255)    NOT NULL COMMENT '课程名称',
    semester_id     BIGINT UNSIGNED NOT NULL COMMENT '关联 semesters.id',
    is_primary      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=兼任 1=主职',
    status          TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_teacher_courses (teacher_id, class_id, course_code, semester_id),
    INDEX idx_teacher_courses_teacher (teacher_id),
    INDEX idx_teacher_courses_class (class_id),
    INDEX idx_teacher_courses_semester (semester_id),
    INDEX idx_teacher_courses_course (course_code),
    CONSTRAINT fk_tc_teacher FOREIGN KEY (teacher_id) REFERENCES users(id),
    CONSTRAINT fk_tc_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_tc_semester FOREIGN KEY (semester_id) REFERENCES semesters(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师授课关系表：记录教师与授课班级、课程的关联';

-- ----------------------------------------------------------------------------
-- 8.2 role_scopes.scope_type 增加课程维度（5=课程）
-- 原范围类型：1=学校 2=学院 3=专业 4=班级
-- 增加课程维度以支持按课程授权（scope_id 对应 teacher_courses.id）
-- ----------------------------------------------------------------------------
ALTER TABLE role_scopes
MODIFY COLUMN scope_type TINYINT UNSIGNED NOT NULL COMMENT '范围类型：1=学校 2=学院 3=专业 4=班级 5=课程';

-- ----------------------------------------------------------------------------
-- 8.3 增加独立的审核员角色
-- 审核员为系统内置角色，可审核档案、奖项报名、职业规划等
-- ----------------------------------------------------------------------------
INSERT INTO roles (name, code, description, level, is_system, is_auditor, scope_types, max_scope_count, status, created_at, updated_at)
SELECT '审核员', 'auditor', '独立审核员角色，可审核档案、奖项、职业规划等', 2, 1, 1, '[1,2,3,4,5]', 0, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'auditor');

-- ----------------------------------------------------------------------------
-- 8.4 增加教师端权限编码
-- 教师工作台菜单及子权限，支持成果热力图、改进建议等功能
-- ----------------------------------------------------------------------------

-- 8.4.1 教师工作台主菜单（父权限）
INSERT INTO permissions (name, code, type, parent_id, sort, status, created_at, updated_at)
SELECT '教师工作台', 'teacher:workspace', 1, NULL, 50, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'teacher:workspace');

-- 8.4.2 子菜单权限
INSERT INTO permissions (name, code, type, parent_id, sort, status, created_at, updated_at)
SELECT '我的班级', 'teacher:class', 1, (SELECT id FROM permissions WHERE code = 'teacher:workspace'), 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'teacher:class');

INSERT INTO permissions (name, code, type, parent_id, sort, status, created_at, updated_at)
SELECT '成果热力图', 'teacher:heatmap', 1, (SELECT id FROM permissions WHERE code = 'teacher:workspace'), 2, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'teacher:heatmap');

INSERT INTO permissions (name, code, type, parent_id, sort, status, created_at, updated_at)
SELECT '学生画像', 'teacher:portrait', 1, (SELECT id FROM permissions WHERE code = 'teacher:workspace'), 3, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'teacher:portrait');

INSERT INTO permissions (name, code, type, parent_id, sort, status, created_at, updated_at)
SELECT '改进建议', 'teacher:suggestion', 1, (SELECT id FROM permissions WHERE code = 'teacher:workspace'), 4, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'teacher:suggestion');

INSERT INTO permissions (name, code, type, parent_id, sort, status, created_at, updated_at)
SELECT '成绩分析', 'teacher:grade:analysis', 1, (SELECT id FROM permissions WHERE code = 'teacher:workspace'), 5, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'teacher:grade:analysis');

INSERT INTO permissions (name, code, type, parent_id, sort, status, created_at, updated_at)
SELECT '成长时间轴', 'teacher:timeline', 1, (SELECT id FROM permissions WHERE code = 'teacher:workspace'), 6, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'teacher:timeline');

-- 8.4.3 按钮级权限
INSERT INTO permissions (name, code, type, parent_id, sort, status, created_at, updated_at)
SELECT '导出数据', 'teacher:export', 2, (SELECT id FROM permissions WHERE code = 'teacher:workspace'), 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'teacher:export');

INSERT INTO permissions (name, code, type, parent_id, sort, status, created_at, updated_at)
SELECT '发送提醒', 'teacher:notify', 2, (SELECT id FROM permissions WHERE code = 'teacher:workspace'), 2, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'teacher:notify');

INSERT INTO permissions (name, code, type, parent_id, sort, status, created_at, updated_at)
SELECT '添加评语', 'teacher:comment', 2, (SELECT id FROM permissions WHERE code = 'teacher:workspace'), 3, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'teacher:comment');

-- ----------------------------------------------------------------------------
-- 8.5 archives 增加 course_code 字段
-- 按需关联课程，位于 semester_id 之后
-- ---------------------------------------------------------------------------
ALTER TABLE archives
ADD COLUMN course_code VARCHAR(50) NULL DEFAULT NULL COMMENT '关联课程编码' AFTER semester_id;

-- ----------------------------------------------------------------------------
-- 8.6 教师-班级-课程关系视图
-- 方便查询教师与班级、课程、专业、学院之间的关联关系
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW teacher_class_relations AS
SELECT
    tc.id               AS relation_id,
    tc.teacher_id       AS teacher_id,
    u.name              AS teacher_name,
    u.user_no           AS teacher_no,
    tc.class_id         AS class_id,
    c.name              AS class_name,
    c.grade             AS class_grade,
    m.id                AS major_id,
    m.name              AS major_name,
    col.id              AS college_id,
    col.name            AS college_name,
    tc.course_code      AS course_code,
    tc.course_name      AS course_name,
    tc.semester_id      AS semester_id,
    s.name              AS semester_name,
    tc.is_primary       AS is_primary,
    tc.status           AS status,
    tc.school_id        AS school_id
FROM teacher_courses tc
JOIN users u ON u.id = tc.teacher_id AND u.deleted_at IS NULL
JOIN classes c ON c.id = tc.class_id AND c.deleted_at IS NULL
JOIN majors m ON m.id = c.major_id AND m.deleted_at IS NULL
JOIN colleges col ON col.id = m.college_id AND col.deleted_at IS NULL
JOIN semesters s ON s.id = tc.semester_id AND s.deleted_at IS NULL;
