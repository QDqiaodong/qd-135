package com.example.heritagetrace.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 传承人在册状态变更请求。目标状态只接受：
 * ACTIVE（在册，重新启用）/ SUSPENDED（停档）。
 * 停档与启用可以来回切换，以最后落成的那一次为准。
 */
@Data
public class InheritorStatusUpdateRequest {
    @NotBlank(message = "目标状态不能为空")
    private String status;
}
