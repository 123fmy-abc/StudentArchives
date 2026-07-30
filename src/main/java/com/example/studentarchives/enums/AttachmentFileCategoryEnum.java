package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 附件文件分类枚举（对齐 attachment_relations.file_category）
 * <p>
 * 学生档案系统表 attachment_relations.file_category
 * certificate/photo/proof/other
 */
@Getter
@AllArgsConstructor
public enum AttachmentFileCategoryEnum {

    CERTIFICATE("certificate", "证书"),
    PHOTO("photo", "照片"),
    PROOF("proof", "证明"),
    OTHER("other", "其他"),
    ;

    private final String value;
    private final String label;

    public static AttachmentFileCategoryEnum of(String value) {
        if (value == null) return null;
        for (AttachmentFileCategoryEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
