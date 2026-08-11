package com.example.studentarchives.dto.Fmy.permission.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限码列表项
 * <p>
 * 对应《管理端接口文档》五、权限控制：权限码统一由后端维护，前端按 code 控制按钮显隐。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionItemResponse {

    /** 权限 ID */
    private Long id;

    /** 权限名称 */
    private String name;

    /** 权限编码（接口/按钮控制用） */
    private String code;

    /** 权限类型：1=菜单 2=按钮 3=接口 4=数据 */
    private Integer type;

    /** 父级权限 ID */
    private Long parentId;

    /** 排序 */
    private Integer sort;

    /** 0=禁用 1=启用 */
    private Integer status;
}
