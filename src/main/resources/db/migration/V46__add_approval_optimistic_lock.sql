-- V46: 审批链路增加乐观锁版本列（BE-PENDING-007 / D07）
-- 背景：审批通过/退回/撤销/转交与撤回均采用「读-判-写」，并发时后写覆盖先写；
--      实体增加 @Version 后由 Hibernate 在 flush 时做版本校验，冲突返回 409。
-- 注意：不要复用业务表已有的 current_version（提交轮次语义），必须使用独立的 lock_version。

ALTER TABLE `pending_approvals`
    ADD COLUMN `lock_version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

ALTER TABLE `approval_instances`
    ADD COLUMN `lock_version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

ALTER TABLE `approval_nodes`
    ADD COLUMN `lock_version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

ALTER TABLE `archives`
    ADD COLUMN `lock_version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

ALTER TABLE `award_applications`
    ADD COLUMN `lock_version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

ALTER TABLE `career_plans`
    ADD COLUMN `lock_version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

-- 兜底：同一审批节点只允许一条待办，防止并发/重复提交生成重复任务。
-- 若历史数据存在 node_id 重复，需先清理脏数据再执行本语句。
ALTER TABLE `pending_approvals`
    ADD UNIQUE KEY `uk_pa_node` (`node_id`, `is_deleted_null`);
