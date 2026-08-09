package com.example.studentarchives.dto.Fmy.profile.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 提交档案导出请求 DTO（POST /profile/export）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {

    /** 导出的栏目 code 列表，不传或为空时默认导出全部栏目 */
    private List<String> sections;

    /** 文件类型：pdf / word，默认 pdf */
    private String fileType = "pdf";

    /** 导出用途：internal（内部查看，带水印）/ external（外部投递，默认，无水印） */
    private String purpose = "external";
}
