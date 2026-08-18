-- ============================================================
-- 字典项新增备注字段（《管理端接口文档》十、10.4 更新字典项支持 remark）
-- ============================================================
ALTER TABLE `dictionaries`
    ADD COLUMN `remark` VARCHAR(255) NULL DEFAULT NULL COMMENT '备注' AFTER `status`;
