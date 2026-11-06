package com.susan.training.web.validation;

/**
 * 用接口作为校验分组标记，避免创建和更新复用请求对象时互相污染规则。
 */
public final class ValidationGroups {
    private ValidationGroups() {
    }

    /**
     * 创建资源时需要校验的字段。
     */
    public interface Create {
    }

    /**
     * 更新资源时需要校验的字段。
     */
    public interface Update {
    }
}
