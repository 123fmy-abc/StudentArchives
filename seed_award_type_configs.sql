-- ============================================================
-- 种子数据：奖项类型配置表（award_type_configs）
--
-- 说明：
-- 1. 覆盖 AwardTypeEnum 中全部 3 种奖项类型。
-- 2. award_type 编码与 AwardTypeEnum.value 一一对应。
-- 3. status=1 表示启用，奖项评选说明（AwardService.getGuide）依赖此表。
-- 4. evaluate_requirements 为对象数组 [{field,label,required,description}]，
--    由 AwardService 后端解析为 List<Requirement>。
-- 5. evaluate_notes 为字符串数组。
-- ============================================================
INSERT INTO `award_type_configs`
(`id`, `award_type`, `type_name`, `evaluate_desc`, `evaluate_requirements`, `evaluate_notes`, `sort`, `status`) VALUES
(1, 'competition_star', '竞赛之星',
 '授予在各类学科竞赛中表现突出、取得优异成绩的学生。',
 '[{"field":"competitionName","label":"竞赛名称","required":true,"description":"参加竞赛的全称"},{"field":"competitionLevel","label":"竞赛级别","required":true,"description":"国家级/省部级/校级/院级"},{"field":"awardLevel","label":"获奖等级","required":true,"description":"如特等奖/一等奖/二等奖"}]',
 '["需上传获奖证书扫描件","证书信息须与获奖事实一致"]',
 1, 1),
(2, 'research_star', '科研之星',
 '授予在科研项目、软件著作权、学术论文等方面取得成果的学生。',
 '[{"field":"primaryCategory","label":"主类别","required":true,"description":"科研项目/软件著作权/发表论文"}]',
 '["可填写多个科研项目、软著、论文子项","需上传立项或成果证明文件"]',
 2, 1),
(3, 'innovation_star', '双创之星',
 '授予在创新创业活动中表现突出、创办或参与企业的学生。',
 '[{"field":"companyName","label":"公司名称","required":true,"description":"所创办或参与企业的全称"},{"field":"industryType","label":"行业类型","required":true,"description":"企业所属行业"}]',
 '["需上传营业执照或项目证明","注明本人在企业中的角色"]',
 3, 1);