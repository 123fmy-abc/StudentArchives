-- ============================================================
-- V32: Relax archive extension NOT NULL constraints
-- 放宽档案扩展表的"分类/级别/内容"类字段 NOT NULL 约束，
-- 使草稿（isDraft=1）可保存中间态（部分字段未填）。
--
-- 说明：
-- 1. 只放宽分类/级别/内容类可选字段，名称字段（如 competition_name、
--    company_name、scholarship_name、certificate_name、book_name 等）仍保持
--    NOT NULL —— 它们是 archives.title 的来源，草稿也至少需要名称。
-- 2. archive_id 外键仍保持 NOT NULL。
-- 3. 提交态（isDraft=0）的必填校验由 Service 层（ApplicationService）负责，
--    返回 400（PARAM_MISSING），不再依赖数据库约束返回 409。
-- ============================================================

-- 1. archive_competitions — 学科竞赛
ALTER TABLE `archive_competitions`
    MODIFY `competition_type` VARCHAR(100) NULL DEFAULT NULL COMMENT '竞赛类型',
    MODIFY `award_level`      VARCHAR(50)  NULL DEFAULT NULL COMMENT '获奖等级';

-- 2. archive_innovations — 创新创业
ALTER TABLE `archive_innovations`
    MODIFY `industry_type` VARCHAR(100) NULL DEFAULT NULL COMMENT '行业类型',
    MODIFY `project_type`  VARCHAR(100) NULL DEFAULT NULL COMMENT '公司类型';

-- 3. archive_researches — 学术研究
ALTER TABLE `archive_researches`
    MODIFY `project_level` VARCHAR(50)  NULL DEFAULT NULL COMMENT '项目级别',
    MODIFY `project_type`  VARCHAR(100) NULL DEFAULT NULL COMMENT '项目类型/研究类型';

-- 4. archive_scholarships — 奖学金
ALTER TABLE `archive_scholarships`
    MODIFY `scholarship_category` VARCHAR(50) NULL DEFAULT NULL COMMENT '奖学金类别',
    MODIFY `award_level`          VARCHAR(50) NULL DEFAULT NULL COMMENT '获奖等级';

-- 5. archive_certificates — 荣誉证书
ALTER TABLE `archive_certificates`
    MODIFY `certificate_type` VARCHAR(100) NULL DEFAULT NULL COMMENT '证书类型';

-- 6. archive_book_reviews — 图书心得
ALTER TABLE `archive_book_reviews`
    MODIFY `read_month`     DATE NULL DEFAULT NULL COMMENT '阅读时间',
    MODIFY `review_content` TEXT NULL DEFAULT NULL COMMENT '心得体会';