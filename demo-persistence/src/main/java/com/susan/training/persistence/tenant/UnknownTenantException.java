package com.susan.training.persistence.tenant;

/**
 * 请求中的租户没有可路由的数据源。
 */
public class UnknownTenantException extends RuntimeException {
    public UnknownTenantException(String tenantId) {
        super("未知租户: " + tenantId);
    }
}
