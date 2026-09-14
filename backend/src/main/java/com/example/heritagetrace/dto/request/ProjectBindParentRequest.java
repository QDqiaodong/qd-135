package com.example.heritagetrace.dto.request;

import lombok.Data;

/**
 * 管理员将子项目挂到父级项目下的请求。
 * parentId 为 null 表示解除挂载、升级为顶级项目。
 */
@Data
public class ProjectBindParentRequest {
    private Long parentId;
}
