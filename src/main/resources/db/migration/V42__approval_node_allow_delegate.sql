-- ============================================================
-- V42: approval_nodes 增加 allow_delegate 快照列
-- 委托契约：委托仅对 approval_flow_steps.allow_delegate=1 的节点生效。
-- 任务分配时（ApprovalSubmitService.createStepNode）已按步骤 gate 改派，
-- 此处把该值快照到节点，供受托人代审放行（AuditTaskService.isInScope）
-- 按任务节点校验，避免「节点未开启委托，受托人仍可见/可代审」。
-- 旧数据默认 0（不允许委托），与既有流程步骤默认 allow_delegate=0 一致。
-- ============================================================

ALTER TABLE `approval_nodes`
    ADD COLUMN `allow_delegate` TINYINT NOT NULL DEFAULT 0 COMMENT '创建节点时步骤快照：0=不允许委托 1=允许委托（受托人代审前置校验）' AFTER `delegation_id`;
