-- V58: archive_competitions 增加 certificate_no 列（证书编号，选填）
-- 背景：学科竞赛申报缺少证书编号，无法与荣誉证书一致地登记证书信息，重复申报检测也缺少比对依据。
-- 方案：新增可空列，申报提交/草稿自动保存/更正时按需写入；非必填，历史数据保持 NULL。

ALTER TABLE `archive_competitions`
    ADD COLUMN `certificate_no` VARCHAR(100) NULL DEFAULT NULL COMMENT '证书编号（选填）' AFTER `participant_role`;
