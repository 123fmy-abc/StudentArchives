-- ============================================================
-- V56: 评选说明配置种子数据
--
-- 背景：
-- 1. archive_type_configs 建表后从未写入任何数据，导致
--    GET /applications/{type}/guide 的正文、申报要求、注意事项始终为空；
-- 2. award_type_configs 的种子数据此前仅存在于手工脚本
--    sql/seed_award_type_configs.sql，未执行该脚本的环境奖项评选说明同样为空。
--
-- 说明：
-- 1. 两张配置表均为全局配置（表结构无 school_id），此处按类型编码补齐；
-- 2. 使用 INSERT IGNORE，已存在同类型配置的环境不会被覆盖。
-- ============================================================

INSERT IGNORE INTO `archive_type_configs`
(`archive_type`, `type_name`, `evaluate_desc`, `evaluate_requirements`, `evaluate_notes`, `apply_desc`, `icon`, `sort`, `status`) VALUES
('academic_competition', '学科竞赛',
 '用于记录学生在校期间参加各级各类学科竞赛并获奖的情况，包括国家级、省部级、校级及院级竞赛。获奖记录经审核通过后计入综合素质评价。',
 '[{"field":"competitionName","label":"竞赛名称","required":true,"description":"请填写竞赛完整名称，如全国大学生数学建模竞赛"},{"field":"competitionType","label":"竞赛类型","required":true,"description":"选择竞赛所属类型"},{"field":"awardLevel","label":"获奖等级","required":true,"description":"填写所获奖项等级"},{"field":"obtainedTime","label":"获奖时间","required":true,"description":"填写证书上的获奖日期"},{"field":"evidenceFiles","label":"佐证材料","required":true,"description":"须上传获奖证书扫描件或清晰照片"}]',
 '["同一竞赛获多项奖项请分别申报","佐证材料须包含证书扫描件或清晰照片","团队获奖请注明本人角色（负责人/成员）","证书编号选填，填写后便于重复检测"]',
 '请按获奖证书如实填写竞赛名称、竞赛类型与获奖等级，并上传证书扫描件。',
 'trophy', 1, 1),
('innovation_entrepreneurship', '创新创业',
 '用于记录学生参与创新创业项目、创办或参与企业等情况，包括创新创业训练计划、创业实践项目及企业创办经历。',
 '[{"field":"companyName","label":"公司/项目名称","required":true,"description":"所创办企业或创新创业项目的全称"},{"field":"industryType","label":"行业类型","required":true,"description":"企业或项目所属行业"},{"field":"projectType","label":"项目类型","required":true,"description":"选择创新创业项目类型"},{"field":"registeredTime","label":"注册/立项时间","required":true,"description":"企业注册或项目立项日期"},{"field":"evidenceFiles","label":"佐证材料","required":true,"description":"须上传营业执照、立项书或参赛证明"}]',
 '["请填写企业或项目的正式全称","需上传营业执照、项目立项书或参赛证明","团队项目请注明本人角色","注册/立项时间不得晚于当前日期"]',
 '请如实填写企业或项目信息，并上传相应证明材料。',
 'rocket', 2, 1),
('academic_research', '学术研究',
 '用于记录学生参与科研项目、课题研究等学术研究经历，仅可申报已完成（结题）的项目。',
 '[{"field":"projectName","label":"项目名称","required":true,"description":"科研项目或课题的完整名称"},{"field":"projectLevel","label":"项目级别","required":true,"description":"国家级/省部级/校级等"},{"field":"projectType","label":"项目类型","required":true,"description":"选择科研项目类型"},{"field":"startDate","label":"开始日期","required":true,"description":"项目开始日期，须早于结束日期"},{"field":"endDate","label":"结束日期","required":true,"description":"项目结题日期，不得晚于当前日期"},{"field":"evidenceFiles","label":"佐证材料","required":true,"description":"须上传结题证明或立项文件"}]',
 '["仅可申报已完成或已结题的项目","开始日期须早于结束日期","结束日期不得晚于当前日期","取得时间以结题时间为准","需上传结题证明或立项文件"]',
 '请填写已完成的科研项目信息，并上传结题或立项证明材料。',
 'flask', 3, 1),
('scholarship', '奖学金',
 '用于记录学生在校期间获得的各类奖学金，包括国家奖学金、国家励志奖学金、校级奖学金及社会奖学金等。',
 '[{"field":"scholarshipName","label":"奖学金名称","required":true,"description":"获奖证书上的奖学金全称"},{"field":"scholarshipCategory","label":"奖学金类别","required":true,"description":"选择奖学金所属类别"},{"field":"awardLevel","label":"获奖等级","required":true,"description":"填写所获奖项等级"},{"field":"obtainedTime","label":"获奖时间","required":true,"description":"填写证书上的获奖日期"},{"field":"evidenceFiles","label":"佐证材料","required":true,"description":"须上传获奖证书或公示文件"}]',
 '["请按获奖证书填写奖学金名称与等级","需上传获奖证书或公示文件","同一学年不同奖学金请分别申报"]',
 '请按证书如实填写奖学金信息，并上传获奖证明材料。',
 'medal', 4, 1),
('honor_certificate', '荣誉证书',
 '用于记录学生在校期间获得的各级各类荣誉证书，如三好学生、优秀学生干部、优秀团员等。',
 '[{"field":"certificateType","label":"证书类型","required":true,"description":"选择荣誉证书类型"},{"field":"certificateName","label":"证书名称","required":true,"description":"证书上的完整名称"},{"field":"obtainedTime","label":"获得时间","required":true,"description":"证书落款日期"},{"field":"issuingUnit","label":"颁发单位","required":true,"description":"证书颁发单位全称"},{"field":"evidenceFiles","label":"佐证材料","required":true,"description":"须上传荣誉证书扫描件"}]',
 '["请填写证书全称与颁发单位","需上传证书扫描件","证书编号选填，填写后便于重复检测"]',
 '请按证书如实填写荣誉信息，并上传证书扫描件。',
 'award', 5, 1),
('internship', '实习经历',
 '用于记录学生在校期间的企业实习、实践基地实习等经历，包括暑期实习、顶岗实习等。',
 '[{"field":"companyName","label":"实习单位","required":true,"description":"实习单位全称"},{"field":"position","label":"实习岗位","required":true,"description":"所担任的实习岗位"},{"field":"location","label":"实习地点","required":true,"description":"实习所在地"},{"field":"startDate","label":"开始日期","required":true,"description":"实习开始日期，须早于结束日期"},{"field":"endDate","label":"结束日期","required":true,"description":"实习结束日期"},{"field":"evidenceFiles","label":"佐证材料","required":true,"description":"须上传实习证明或实习协议"}]',
 '["实习开始日期须早于结束日期","需上传实习证明或实习协议","请填写实习单位全称与岗位"]',
 '请如实填写实习单位、岗位与起止时间，并上传实习证明。',
 'briefcase', 6, 1),
('organization', '组织履历',
 '用于记录学生在校期间担任学生干部、参加学生组织等履历，包括学生会、社团、班委等任职经历。',
 '[{"field":"orgLevel","label":"组织层级","required":true,"description":"校级/院级/班级等"},{"field":"department","label":"所属部门","required":true,"description":"组织内所属部门"},{"field":"positionTitle","label":"担任职务","required":true,"description":"所担任的具体职务"},{"field":"startDate","label":"任职开始日期","required":true,"description":"须早于结束日期"},{"field":"endDate","label":"任职结束日期","required":true,"description":"任职结束日期"},{"field":"evidenceFiles","label":"佐证材料","required":true,"description":"须上传任职证明或聘书"}]',
 '["请填写组织层级、部门与职务","任职开始日期须早于结束日期","需上传任职证明或聘书"]',
 '请如实填写任职信息，并上传任职证明材料。',
 'users', 7, 1),
('training_project', '实训项目',
 '用于记录学生参加的校内实训、课程设计、实验项目等实践训练项目。',
 '[{"field":"projectName","label":"项目名称","required":true,"description":"实训项目具体名称"},{"field":"projectContent","label":"项目内容","required":true,"description":"概述项目主要内容与本人工作"},{"field":"startDate","label":"开始日期","required":true,"description":"须早于结束日期"},{"field":"endDate","label":"结束日期","required":true,"description":"项目结束日期"},{"field":"evidenceFiles","label":"佐证材料","required":true,"description":"须上传项目成果或结业证明"}]',
 '["请概述项目内容与本人承担的工作","开始日期须早于结束日期","需上传项目成果或结业证明"]',
 '请填写实训项目信息，并上传项目成果或结业证明。',
 'tools', 8, 1),
('social_practice', '社会实践',
 '用于记录学生参加志愿服务、社会调研、支教等社会实践活动的情况。',
 '[{"field":"activityName","label":"活动名称","required":true,"description":"社会实践活动的完整名称"},{"field":"practiceLocation","label":"实践地点","required":true,"description":"活动开展地点"},{"field":"practiceUnit","label":"实践单位","required":true,"description":"组织或接收单位全称"},{"field":"startDate","label":"开始日期","required":true,"description":"须早于结束日期"},{"field":"endDate","label":"结束日期","required":true,"description":"活动结束日期"},{"field":"evidenceFiles","label":"佐证材料","required":true,"description":"须上传实践证明或活动照片"}]',
 '["志愿时长可填写 0 或留空","需上传实践证明或活动照片","团队实践请注明本人角色"]',
 '请如实填写社会实践活动信息，并上传实践证明材料。',
 'heart', 9, 1),
('book_review', '图书心得',
 '用于记录学生阅读图书并撰写读书心得的情况，鼓励形成持续的阅读与思考习惯。',
 '[{"field":"bookName","label":"图书名称","required":true,"description":"所阅读图书的完整名称"},{"field":"readMonth","label":"阅读月份","required":true,"description":"阅读该书的月份"},{"field":"reviewContent","label":"读书心得","required":true,"description":"本人撰写的读书心得正文"},{"field":"evidenceFiles","label":"佐证材料","required":true,"description":"建议上传阅读笔记或图书封面"}]',
 '["请填写图书全名与阅读月份","心得须为本人原创","建议上传阅读笔记或图书封面"]',
 '请填写图书信息与本人原创心得内容。',
 'book', 10, 1);

INSERT IGNORE INTO `award_type_configs`
(`award_type`, `type_name`, `evaluate_desc`, `evaluate_requirements`, `evaluate_notes`, `apply_desc`, `icon`, `sort`, `status`) VALUES
('competition_star', '竞赛之星',
 '授予在各类学科竞赛中表现突出、取得优异成绩的学生。',
 '[{"field":"competitionName","label":"竞赛名称","required":true,"description":"参加竞赛的全称"},{"field":"competitionLevel","label":"竞赛级别","required":true,"description":"国家级/省部级/校级/院级"},{"field":"awardLevel","label":"获奖等级","required":true,"description":"如特等奖/一等奖/二等奖"},{"field":"participatedTime","label":"参赛时间","required":true,"description":"实际参赛日期"},{"field":"evidenceFiles","label":"佐证材料","required":true,"description":"须上传获奖证书或参赛证明"}]',
 '["竞赛之星每人每学期限申报一项","需上传获奖证书扫描件","证书信息须与获奖事实一致","团队获奖须注明本人排名"]',
 '请如实填写竞赛与获奖信息，并上传获奖证明材料。',
 'trophy', 1, 1),
('research_star', '科研之星',
 '授予在科研项目、软件著作权、学术论文等方面取得成果的学生。',
 '[{"field":"primaryCategory","label":"主类别","required":true,"description":"科研项目/软件著作权/发表论文"},{"field":"evidenceFiles","label":"佐证材料","required":true,"description":"须上传立项或成果证明文件"}]',
 '["可填写多个科研项目、软著、论文子项","需上传立项或成果证明文件","成果取得时间不得晚于所选学期结束日期"]',
 '请选择主类别并填写科研成果信息，上传相应证明材料。',
 'flask', 2, 1),
('innovation_star', '双创之星',
 '授予在创新创业活动中表现突出、创办或参与企业的学生。',
 '[{"field":"companyName","label":"公司名称","required":true,"description":"所创办或参与企业的全称"},{"field":"industryType","label":"行业类型","required":true,"description":"企业所属行业"},{"field":"evidenceFiles","label":"佐证材料","required":true,"description":"须上传营业执照或项目证明"}]',
 '["需上传营业执照或项目证明","注明本人在企业中的角色","企业注册时间不得晚于所选学期结束日期"]',
 '请如实填写企业与项目信息，并上传相应证明材料。',
 'rocket', 3, 1);
