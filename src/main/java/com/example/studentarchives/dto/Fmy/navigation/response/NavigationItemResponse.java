package com.example.studentarchives.dto.Fmy.navigation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 导航菜单项
 * <p>
 * 后端按当前用户角色聚合菜单树，将图片中的菜单层级与后端接口模块一一映射，
 * 避免前端硬编码菜单路径导致与接口文档不一致。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationItemResponse {

    /** 菜单唯一标识 */
    private String key;

    /** 菜单名称 */
    private String name;

    /** 前端路由或外链 */
    private String path;

    /** 图标标识 */
    private String icon;

    /** 所需权限码（空表示仅需登录） */
    private String requiredPermission;

    /** 子菜单 */
    private List<NavigationItemResponse> children;
}
