package com.susan.training.web.database;

import com.susan.training.common.ApiResponse;
import com.susan.training.persistence.tenant.TenantDirectoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Schema-per-tenant 动态数据源演示接口。
 */
@RestController
@RequestMapping("/api/v1/db/tenant-notes")
@Profile("db")
@Tag(name = "多租户动态数据源", description = "Schema-per-tenant 路由与隔离")
public class TenantDirectoryController {
    private final TenantDirectoryService tenantDirectoryService;

    public TenantDirectoryController(TenantDirectoryService tenantDirectoryService) {
        this.tenantDirectoryService = tenantDirectoryService;
    }

    @PutMapping("/{key}")
    @Operation(summary = "写入租户记录", description = "请求头 X-Tenant-Id 决定目标 Schema")
    public ApiResponse<TenantDirectoryService.TenantNote> put(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable @NotBlank String key,
            @RequestParam @NotBlank String value
    ) {
        return ApiResponse.success(tenantDirectoryService.upsert(key, value));
    }

    @GetMapping
    @Operation(summary = "读取当前租户记录")
    public ApiResponse<List<TenantDirectoryService.TenantNote>> list() {
        return ApiResponse.success(tenantDirectoryService.findAll());
    }
}
