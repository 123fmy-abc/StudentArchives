-- 头像字段存 OSS 预签名 URL（含签名、过期时间等查询参数，长度可超 500 字符），
-- 原 VARCHAR(255) 导致落库报 Data truncation: Data too long for column 'avatar'
ALTER TABLE `user_contact_infos`
    MODIFY COLUMN `avatar` VARCHAR(1024) NULL DEFAULT NULL COMMENT '头像 URL（OSS 预签名地址）';
