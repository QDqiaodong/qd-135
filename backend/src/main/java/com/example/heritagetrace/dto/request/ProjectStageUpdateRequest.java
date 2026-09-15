package com.example.heritagetrace.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 项目阶段推进请求。阶段只能顺着走：在研 → 送审 → 结项，
 * 不能跳级也不能退回，目标阶段必须是当前阶段的下一级。
 */
@Data
public class ProjectStageUpdateRequest {
    @NotBlank(message = "目标阶段不能为空")
    private String stage;
}
