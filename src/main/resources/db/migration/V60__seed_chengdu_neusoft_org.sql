-- ============================================================
-- V60: 成都东软学院真实组织架构（学院 / 专业 / 班级）
--
-- 数据来源：
--   1. 成都东软学院官网「学院介绍」 https://nsu.edu.cn/HTML/dept/
--   2. 2026 年招生章程（本科招生专业）
--   3. 2026 年新生入学须知（专业与二级学院对应关系）
--
-- 说明：
--   * 仅收录有招生专业的 7 个二级学院；马克思主义学院、创新创业学院、
--     继续教育（培训）学院、退役军人教育学院、国际教育学院、数据科学与基础学院
--     不设招生专业，级联选择器中不会出现，故不写入。
--   * 班级名称官网未公开，这里按「专业名 + 年级 + 1班」生成 2023~2026 级，
--     实际班级可由管理员在后台调整。
--   * 原有的演示组织（华中科技大学及其学院/专业/班级）统一置为停用，
--     保留数据以维持既有用户/档案的外键关系。
--
-- 可重复执行：插入均按唯一键去重，班级按「专业 + 班级名」去重。
-- ============================================================

SET @school_id = (
    SELECT id FROM schools
    WHERE deleted_at IS NULL
    ORDER BY id
    LIMIT 1
);

UPDATE schools
SET name = '成都东软学院',
    code = 'CDNSU'
WHERE id = @school_id;

-- 停用旧演示组织（保留记录，避免破坏既有用户/档案外键）
UPDATE classes cl
JOIN majors m ON cl.major_id = m.id
JOIN colleges c ON m.college_id = c.id
SET cl.status = 0
WHERE c.school_id = @school_id;

UPDATE majors m
JOIN colleges c ON m.college_id = c.id
SET m.status = 0
WHERE c.school_id = @school_id;

UPDATE colleges
SET status = 0
WHERE school_id = @school_id;

-- ============================================================
-- 1. 二级学院
-- ============================================================
INSERT INTO colleges (school_id, name, code, status) VALUES
(@school_id, '计算机与软件学院',     'CS',   1),
(@school_id, '智能科学与工程学院',   'ISE',  1),
(@school_id, '信息与商务管理学院',   'SIBM', 1),
(@school_id, '数字艺术与设计学院',   'DA',   1),
(@school_id, '外国语学院',           'FL',   1),
(@school_id, '健康医疗科技学院',     'HMT',  1),
(@school_id, '数智应用技术学院',     'SZYY', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;

SET @c_cs   = (SELECT id FROM colleges WHERE school_id = @school_id AND code = 'CS'   AND deleted_at IS NULL LIMIT 1);
SET @c_ise  = (SELECT id FROM colleges WHERE school_id = @school_id AND code = 'ISE'  AND deleted_at IS NULL LIMIT 1);
SET @c_sibm = (SELECT id FROM colleges WHERE school_id = @school_id AND code = 'SIBM' AND deleted_at IS NULL LIMIT 1);
SET @c_da   = (SELECT id FROM colleges WHERE school_id = @school_id AND code = 'DA'   AND deleted_at IS NULL LIMIT 1);
SET @c_fl   = (SELECT id FROM colleges WHERE school_id = @school_id AND code = 'FL'   AND deleted_at IS NULL LIMIT 1);
SET @c_hmt  = (SELECT id FROM colleges WHERE school_id = @school_id AND code = 'HMT'  AND deleted_at IS NULL LIMIT 1);
SET @c_szyy = (SELECT id FROM colleges WHERE school_id = @school_id AND code = 'SZYY' AND deleted_at IS NULL LIMIT 1);

-- ============================================================
-- 2. 专业（2026 年招生专业）
-- ============================================================
INSERT INTO majors (college_id, name, code, status) VALUES
-- 计算机与软件学院
(@c_cs, '软件工程',                         'RJGC',    1),
(@c_cs, '计算机科学与技术',                 'JSJ',     1),
(@c_cs, '网络工程',                         'WLGC',    1),
(@c_cs, '物联网工程',                       'WLW',     1),
(@c_cs, '网络空间安全',                     'WLAQ',    1),
-- 智能科学与工程学院
(@c_ise, '人工智能',                        'AI',      1),
(@c_ise, '智能科学与技术',                  'ZNKX',    1),
(@c_ise, '信息工程',                        'XXGC',    1),
(@c_ise, '数据科学与大数据技术',            'SJKX',    1),
(@c_ise, '数据科学与大数据技术（中外合作办学）', 'SJKXZW', 1),
(@c_ise, '大数据管理与应用',                'DSJGL',   1),
(@c_ise, '机器人工程',                      'JQR',     1),
(@c_ise, '集成电路设计与集成系统',          'JCDL',    1),
-- 信息与商务管理学院
(@c_sibm, '信息管理与信息系统',             'XXGL',    1),
(@c_sibm, '电子商务',                       'DZSW',    1),
(@c_sibm, '财务管理',                       'CWGL',    1),
(@c_sibm, '人力资源管理',                   'RLZY',    1),
(@c_sibm, '供应链管理',                     'GYL',     1),
-- 数字艺术与设计学院
(@c_da, '数字媒体技术',                     'SZMT',    1),
(@c_da, '工业设计',                         'GYSJ',    1),
(@c_da, '虚拟现实技术',                     'XNSJ',    1),
(@c_da, '动画',                             'DH',      1),
(@c_da, '艺术与科技',                       'YSKJ',    1),
(@c_da, '数字媒体艺术',                     'SZMS',    1),
(@c_da, '视觉传达设计',                     'SJCD',    1),
-- 外国语学院
(@c_fl, '英语',                             'YY',      1),
(@c_fl, '商务英语',                         'SWYY',    1),
(@c_fl, '日语',                             'RY',      1),
-- 健康医疗科技学院
(@c_hmt, '健康服务与管理',                  'JKFW',    1),
(@c_hmt, '医学影像技术',                    'YXYX',    1),
(@c_hmt, '智能医学工程',                    'ZNYX',    1),
(@c_hmt, '医学信息工程',                    'YXXX',    1),
-- 数智应用技术学院（对口本科 + 专科）
(@c_szyy, '软件工程技术（对口本科）',       'RJGC_DK', 1),
(@c_szyy, '人工智能（对口本科）',           'AI_DK',   1),
(@c_szyy, '医养照护与管理（对口本科）',     'YYZH_DK', 1),
(@c_szyy, '数字动画（对口本科）',           'SZDH_DK', 1),
(@c_szyy, '计算机网络技术',                 'JSJWL_ZK', 1),
(@c_szyy, '信息安全与管理',                 'XXAQ_ZK',  1),
(@c_szyy, '大数据技术与应用',               'DSJ_ZK',   1),
(@c_szyy, '软件技术',                       'RJJS_ZK',  1),
(@c_szyy, '电子商务',                       'DZSW_ZK',  1),
(@c_szyy, '现代物流管理',                   'XDWL_ZK',  1),
(@c_szyy, '财务管理',                       'CWGL_ZK',  1),
(@c_szyy, '市场营销',                       'SCYX_ZK',  1),
(@c_szyy, '影视动画',                       'YSDH_ZK',  1),
(@c_szyy, '产品艺术设计',                   'CPYS_ZK',  1),
(@c_szyy, '应用英语',                       'YYYY_ZK',  1),
(@c_szyy, '应用日语',                       'YYRY_ZK',  1)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;

-- ============================================================
-- 3. 班级：每个专业生成 2023~2026 级 1 班
-- ============================================================
INSERT INTO classes (major_id, name, grade, student_count, status)
SELECT m.id,
       CONCAT(m.name, g.grade, '1班'),
       g.grade,
       0,
       1
FROM majors m
JOIN colleges c ON c.id = m.college_id
CROSS JOIN (
    SELECT '2023级' AS grade
    UNION ALL SELECT '2024级'
    UNION ALL SELECT '2025级'
    UNION ALL SELECT '2026级'
) g
WHERE c.school_id = @school_id
  AND c.status = 1
  AND NOT EXISTS (
      SELECT 1 FROM classes cl
      WHERE cl.major_id = m.id
        AND cl.name = CONCAT(m.name, g.grade, '1班')
        AND cl.deleted_at IS NULL
  );

-- 重复执行时，上面「停用旧组织」会连带停掉已存在的班级，这里恢复启用分支下的班级
UPDATE classes cl
JOIN majors m ON cl.major_id = m.id
JOIN colleges c ON m.college_id = c.id
SET cl.status = 1
WHERE c.school_id = @school_id
  AND c.status = 1
  AND m.status = 1;
